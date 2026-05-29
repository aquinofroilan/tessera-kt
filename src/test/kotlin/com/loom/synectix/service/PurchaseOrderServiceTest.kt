package com.loom.synectix.service

import com.loom.synectix.dto.BillMatchLineRequest
import com.loom.synectix.dto.BillMatchRequest
import com.loom.synectix.dto.CreateBillRequest
import com.loom.synectix.dto.CreatePurchaseOrderLineRequest
import com.loom.synectix.dto.CreatePurchaseOrderRequest
import com.loom.synectix.dto.GenerateBillLine
import com.loom.synectix.dto.GenerateBillRequest
import com.loom.synectix.dto.MatchStatus
import com.loom.synectix.dto.ReceivePurchaseOrderLine
import com.loom.synectix.dto.ReceivePurchaseOrderRequest
import com.loom.synectix.exception.BusinessRuleException
import com.loom.synectix.model.Account
import com.loom.synectix.model.AccountType
import com.loom.synectix.model.Bill
import com.loom.synectix.model.Product
import com.loom.synectix.model.PurchaseOrder
import com.loom.synectix.model.PurchaseOrderStatus
import com.loom.synectix.model.StockMovementType
import com.loom.synectix.model.Vendor
import com.loom.synectix.model.Warehouse
import com.loom.synectix.repository.AccountRepository
import com.loom.synectix.repository.PurchaseOrderRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

class PurchaseOrderServiceTest {
    private lateinit var repository: PurchaseOrderRepository
    private lateinit var vendorService: VendorService
    private lateinit var warehouseService: WarehouseService
    private lateinit var productService: ProductService
    private lateinit var stockMovementService: StockMovementService
    private lateinit var accountRepository: AccountRepository
    private lateinit var billService: BillService
    private lateinit var service: PurchaseOrderService

    private val orgId = "org-1"
    private val userId = "user-1"

    @BeforeEach
    fun setup() {
        repository = mock(PurchaseOrderRepository::class.java)
        vendorService = mock(VendorService::class.java)
        warehouseService = mock(WarehouseService::class.java)
        productService = mock(ProductService::class.java)
        stockMovementService = mock(StockMovementService::class.java)
        accountRepository = mock(AccountRepository::class.java)
        billService = mock(BillService::class.java)
        whenever(repository.countByOrganizationId(orgId)).thenReturn(0L)
        whenever(repository.save(any<PurchaseOrder>())).thenAnswer { it.arguments[0] }
        whenever(vendorService.getVendor("v-1", orgId)).thenReturn(Vendor(id = "v-1", name = "Acme", organizationId = orgId))
        whenever(warehouseService.getWarehouse("wh-1", orgId))
            .thenReturn(Warehouse(id = "wh-1", code = "MAIN", name = "Main", organizationId = orgId))
        whenever(productService.getProduct("p-1", orgId)).thenReturn(
            Product(id = "p-1", sku = "SKU-1", name = "Widget", listPrice = BigDecimal("9"), priceCurrency = "USD", organizationId = orgId),
        )
        service =
            PurchaseOrderService(
                repository,
                vendorService,
                warehouseService,
                productService,
                stockMovementService,
                accountRepository,
                billService,
            )
    }

    private fun createRequest() =
        CreatePurchaseOrderRequest(
            vendorId = "v-1",
            warehouseId = "wh-1",
            orderDate = LocalDate.of(2026, 5, 1),
            lines = listOf(CreatePurchaseOrderLineRequest(productId = "p-1", quantity = BigDecimal("10"), unitCost = BigDecimal("5"))),
        )

    @Test
    fun `create computes line totals and order total and starts in DRAFT`() {
        val po = service.createPurchaseOrder(createRequest(), orgId, userId)

        assertThat(po.poNumber).isEqualTo("PO-0001")
        assertThat(po.status).isEqualTo(PurchaseOrderStatus.DRAFT)
        assertThat(po.lines).hasSize(1)
        assertThat(po.lines.first().lineTotal).isEqualByComparingTo("50")
        assertThat(po.totalAmount).isEqualByComparingTo("50")
        assertThat(po.lines.first().productSku).isEqualTo("SKU-1")
    }

    @Test
    fun `create rejects inactive vendor`() {
        whenever(vendorService.getVendor("v-1", orgId))
            .thenReturn(Vendor(id = "v-1", name = "Acme", organizationId = orgId, isActive = false))

        assertThatThrownBy { service.createPurchaseOrder(createRequest(), orgId, userId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `receive posts a RECEIPT stock movement per line and marks RECEIVED`() {
        val approved = service.createPurchaseOrder(createRequest(), orgId, userId).copy(status = PurchaseOrderStatus.APPROVED)
        whenever(repository.findById(approved.id)).thenReturn(java.util.Optional.of(approved))

        val received = service.receivePurchaseOrder(approved.id, null, orgId, userId)

        assertThat(received.status).isEqualTo(PurchaseOrderStatus.RECEIVED)
        verify(stockMovementService, times(1)).createMovement(
            argThat { type == StockMovementType.RECEIPT && warehouseId == "wh-1" && productId == "p-1" },
            eq(orgId),
            eq(userId),
        )
    }

    @Test
    fun `receive is rejected unless APPROVED`() {
        val draft = service.createPurchaseOrder(createRequest(), orgId, userId)
        whenever(repository.findById(draft.id)).thenReturn(java.util.Optional.of(draft))

        assertThatThrownBy { service.receivePurchaseOrder(draft.id, null, orgId, userId) }
            .isInstanceOf(BusinessRuleException::class.java)
        verify(stockMovementService, never()).createMovement(any(), any(), any())
    }

    @Test
    fun `partial receive leaves the order PARTIALLY_RECEIVED`() {
        val approved = service.createPurchaseOrder(createRequest(), orgId, userId).copy(status = PurchaseOrderStatus.APPROVED)
        val lineId = approved.lines.first().id
        whenever(repository.findById(approved.id)).thenReturn(Optional.of(approved))

        val result =
            service.receivePurchaseOrder(
                approved.id,
                ReceivePurchaseOrderRequest(listOf(ReceivePurchaseOrderLine(lineId, BigDecimal("4")))),
                orgId,
                userId,
            )

        assertThat(result.status).isEqualTo(PurchaseOrderStatus.PARTIALLY_RECEIVED)
        assertThat(result.lines.first().receivedQuantity).isEqualByComparingTo("4")
    }

    @Test
    fun `receiving more than ordered is rejected`() {
        val approved = service.createPurchaseOrder(createRequest(), orgId, userId).copy(status = PurchaseOrderStatus.APPROVED)
        val lineId = approved.lines.first().id
        whenever(repository.findById(approved.id)).thenReturn(Optional.of(approved))

        assertThatThrownBy {
            service.receivePurchaseOrder(
                approved.id,
                ReceivePurchaseOrderRequest(listOf(ReceivePurchaseOrderLine(lineId, BigDecimal("11")))),
                orgId,
                userId,
            )
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `generateBill bills received quantity to the inventory clearing account`() {
        val received =
            service
                .createPurchaseOrder(createRequest(), orgId, userId)
                .let { po ->
                    po.copy(status = PurchaseOrderStatus.RECEIVED, lines = po.lines.map { it.copy(receivedQuantity = it.quantity) })
                }
        whenever(repository.findById(received.id)).thenReturn(Optional.of(received))
        whenever(accountRepository.findByOrganizationIdAndCode(orgId, "2150"))
            .thenReturn(
                Optional.of(
                    Account(
                        id = "acc-2150",
                        code = "2150",
                        name = "Inventory Clearing",
                        type = AccountType.LIABILITY,
                        organizationId = orgId,
                    ),
                ),
            )
        whenever(billService.createBill(any(), eq(orgId), any())).thenReturn(mock(Bill::class.java))

        service.generateBill(received.id, null, orgId, userId)

        val captor = argumentCaptor<CreateBillRequest>()
        verify(billService).createBill(captor.capture(), eq(orgId), any())
        val billReq = captor.firstValue
        assertThat(billReq.lines).hasSize(1)
        assertThat(billReq.lines.first().accountId).isEqualTo("acc-2150")
        assertThat(billReq.lines.first().amount).isEqualByComparingTo("50")
    }

    @Test
    fun `generateBill rejects a unit cost beyond price tolerance`() {
        val received =
            service
                .createPurchaseOrder(createRequest(), orgId, userId)
                .let { po ->
                    po.copy(status = PurchaseOrderStatus.RECEIVED, lines = po.lines.map { it.copy(receivedQuantity = it.quantity) })
                }
        val lineId = received.lines.first().id
        whenever(repository.findById(received.id)).thenReturn(Optional.of(received))
        whenever(accountRepository.findByOrganizationIdAndCode(orgId, "2150"))
            .thenReturn(
                Optional.of(
                    Account(
                        id = "acc-2150",
                        code = "2150",
                        name = "Inventory Clearing",
                        type = AccountType.LIABILITY,
                        organizationId = orgId,
                    ),
                ),
            )

        // PO cost is 5; billing at 10 is a 100% variance, well beyond the 5% tolerance.
        assertThatThrownBy {
            service.generateBill(
                received.id,
                GenerateBillRequest(listOf(GenerateBillLine(lineId, BigDecimal("10"), BigDecimal("10")))),
                orgId,
                userId,
            )
        }.isInstanceOf(BusinessRuleException::class.java)
        verify(billService, never()).createBill(any(), any(), any())
    }

    @Test
    fun `cancel of a received order reverses stock by reference`() {
        val received =
            service
                .createPurchaseOrder(createRequest(), orgId, userId)
                .let { po ->
                    po.copy(status = PurchaseOrderStatus.RECEIVED, lines = po.lines.map { it.copy(receivedQuantity = it.quantity) })
                }
        whenever(repository.findById(received.id)).thenReturn(Optional.of(received))

        val result = service.cancelPurchaseOrder(received.id, orgId, userId)

        assertThat(result.status).isEqualTo(PurchaseOrderStatus.CANCELLED)
        verify(stockMovementService).reverseByReference(eq("PO-${received.poNumber}"), eq(orgId), eq(userId))
    }

    @Test
    fun `cancel is blocked once a bill has been generated`() {
        val billed =
            service
                .createPurchaseOrder(createRequest(), orgId, userId)
                .let { po ->
                    po.copy(
                        status = PurchaseOrderStatus.RECEIVED,
                        lines = po.lines.map { it.copy(receivedQuantity = it.quantity, billedQuantity = it.quantity) },
                    )
                }
        whenever(repository.findById(billed.id)).thenReturn(Optional.of(billed))

        assertThatThrownBy { service.cancelPurchaseOrder(billed.id, orgId, userId) }
            .isInstanceOf(BusinessRuleException::class.java)
        verify(stockMovementService, never()).reverseByReference(any(), any(), any())
    }

    @Test
    fun `match preview flags price variance and over-billing`() {
        val received =
            service
                .createPurchaseOrder(createRequest(), orgId, userId)
                .let { po ->
                    po.copy(status = PurchaseOrderStatus.RECEIVED, lines = po.lines.map { it.copy(receivedQuantity = it.quantity) })
                }
        val lineId = received.lines.first().id
        whenever(repository.findById(received.id)).thenReturn(Optional.of(received))

        // PO: qty 10 @ 5, fully received. Vendor bills 12 @ 5.10 -> over-billed AND >5% price variance.
        val overBilled =
            service.previewBillMatch(
                received.id,
                BillMatchRequest(listOf(BillMatchLineRequest(lineId, BigDecimal("12"), BigDecimal("5.10")))),
                orgId,
            )
        assertThat(overBilled.matched).isFalse()
        assertThat(overBilled.lines.first().status).isEqualTo(MatchStatus.OVER_BILLED)
        assertThat(overBilled.lines.first().billableQuantity).isEqualByComparingTo("10")

        // Vendor bills 10 @ 5.10 -> within qty, but 2% price variance is within the 5% tolerance.
        val matched =
            service.previewBillMatch(
                received.id,
                BillMatchRequest(listOf(BillMatchLineRequest(lineId, BigDecimal("10"), BigDecimal("5.10")))),
                orgId,
            )
        assertThat(matched.matched).isTrue()
        assertThat(matched.lines.first().status).isEqualTo(MatchStatus.MATCHED)

        // Vendor bills 10 @ 6 -> 20% price variance, beyond tolerance.
        val priceVariance =
            service.previewBillMatch(
                received.id,
                BillMatchRequest(listOf(BillMatchLineRequest(lineId, BigDecimal("10"), BigDecimal("6")))),
                orgId,
            )
        assertThat(priceVariance.lines.first().status).isEqualTo(MatchStatus.PRICE_VARIANCE)
    }
}
