package com.loom.synectix.service

import com.loom.synectix.dto.CreatePurchaseOrderLineRequest
import com.loom.synectix.dto.CreatePurchaseOrderRequest
import com.loom.synectix.exception.BusinessRuleException
import com.loom.synectix.model.Product
import com.loom.synectix.model.PurchaseOrder
import com.loom.synectix.model.PurchaseOrderStatus
import com.loom.synectix.model.StockMovementType
import com.loom.synectix.model.Vendor
import com.loom.synectix.model.Warehouse
import com.loom.synectix.repository.PurchaseOrderRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate

class PurchaseOrderServiceTest {
    private lateinit var repository: PurchaseOrderRepository
    private lateinit var vendorService: VendorService
    private lateinit var warehouseService: WarehouseService
    private lateinit var productService: ProductService
    private lateinit var stockMovementService: StockMovementService
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
        whenever(repository.countByOrganizationId(orgId)).thenReturn(0L)
        whenever(repository.save(any<PurchaseOrder>())).thenAnswer { it.arguments[0] }
        whenever(vendorService.getVendor("v-1", orgId)).thenReturn(Vendor(id = "v-1", name = "Acme", organizationId = orgId))
        whenever(warehouseService.getWarehouse("wh-1", orgId))
            .thenReturn(Warehouse(id = "wh-1", code = "MAIN", name = "Main", organizationId = orgId))
        whenever(productService.getProduct("p-1", orgId)).thenReturn(
            Product(id = "p-1", sku = "SKU-1", name = "Widget", listPrice = BigDecimal("9"), priceCurrency = "USD", organizationId = orgId),
        )
        service = PurchaseOrderService(repository, vendorService, warehouseService, productService, stockMovementService)
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

        val received = service.receivePurchaseOrder(approved.id, orgId, userId)

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

        assertThatThrownBy { service.receivePurchaseOrder(draft.id, orgId, userId) }
            .isInstanceOf(BusinessRuleException::class.java)
        verify(stockMovementService, never()).createMovement(any(), any(), any())
    }
}
