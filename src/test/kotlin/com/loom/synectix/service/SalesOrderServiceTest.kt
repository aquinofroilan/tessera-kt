package com.loom.synectix.service

import com.loom.synectix.dto.CreateInvoiceRequest
import com.loom.synectix.dto.CreateSalesOrderLineRequest
import com.loom.synectix.dto.CreateSalesOrderRequest
import com.loom.synectix.dto.FulfillSalesOrderLine
import com.loom.synectix.dto.FulfillSalesOrderRequest
import com.loom.synectix.exception.BusinessRuleException
import com.loom.synectix.model.Customer
import com.loom.synectix.model.Invoice
import com.loom.synectix.model.Product
import com.loom.synectix.model.SalesOrder
import com.loom.synectix.model.SalesOrderStatus
import com.loom.synectix.model.StockMovementType
import com.loom.synectix.model.Warehouse
import com.loom.synectix.repository.SalesOrderRepository
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

class SalesOrderServiceTest {
    private lateinit var repository: SalesOrderRepository
    private lateinit var customerService: CustomerService
    private lateinit var warehouseService: WarehouseService
    private lateinit var productService: ProductService
    private lateinit var stockMovementService: StockMovementService
    private lateinit var invoiceService: InvoiceService
    private lateinit var service: SalesOrderService

    private val orgId = "org-1"
    private val userId = "user-1"

    @BeforeEach
    fun setup() {
        repository = mock(SalesOrderRepository::class.java)
        customerService = mock(CustomerService::class.java)
        warehouseService = mock(WarehouseService::class.java)
        productService = mock(ProductService::class.java)
        stockMovementService = mock(StockMovementService::class.java)
        invoiceService = mock(InvoiceService::class.java)
        whenever(repository.countByOrganizationId(orgId)).thenReturn(0L)
        whenever(repository.save(any<SalesOrder>())).thenAnswer { it.arguments[0] }
        whenever(customerService.getCustomer("c-1", orgId)).thenReturn(Customer(id = "c-1", name = "Buyer", organizationId = orgId))
        whenever(warehouseService.getWarehouse("wh-1", orgId))
            .thenReturn(Warehouse(id = "wh-1", code = "MAIN", name = "Main", organizationId = orgId))
        whenever(productService.getProduct("p-1", orgId)).thenReturn(
            Product(id = "p-1", sku = "SKU-1", name = "Widget", listPrice = BigDecimal("9"), priceCurrency = "USD", organizationId = orgId),
        )
        service =
            SalesOrderService(
                repository,
                customerService,
                warehouseService,
                productService,
                stockMovementService,
                invoiceService,
            )
    }

    private fun createRequest() =
        CreateSalesOrderRequest(
            customerId = "c-1",
            warehouseId = "wh-1",
            orderDate = LocalDate.of(2026, 5, 1),
            lines = listOf(CreateSalesOrderLineRequest(productId = "p-1", quantity = BigDecimal("4"), unitPrice = BigDecimal("9"))),
        )

    @Test
    fun `create computes totals and starts in DRAFT`() {
        val so = service.createSalesOrder(createRequest(), orgId, userId)

        assertThat(so.soNumber).isEqualTo("SO-0001")
        assertThat(so.status).isEqualTo(SalesOrderStatus.DRAFT)
        assertThat(so.lines.first().lineTotal).isEqualByComparingTo("36")
        assertThat(so.totalAmount).isEqualByComparingTo("36")
    }

    @Test
    fun `fulfill posts an ISSUE stock movement per line and marks FULFILLED`() {
        val approved = service.createSalesOrder(createRequest(), orgId, userId).copy(status = SalesOrderStatus.APPROVED)
        whenever(repository.findById(approved.id)).thenReturn(Optional.of(approved))

        val fulfilled = service.fulfillSalesOrder(approved.id, null, orgId, userId)

        assertThat(fulfilled.status).isEqualTo(SalesOrderStatus.FULFILLED)
        verify(stockMovementService, times(1)).createMovement(
            argThat { type == StockMovementType.ISSUE && warehouseId == "wh-1" && productId == "p-1" },
            eq(orgId),
            eq(userId),
        )
    }

    @Test
    fun `fulfill is rejected unless APPROVED`() {
        val draft = service.createSalesOrder(createRequest(), orgId, userId)
        whenever(repository.findById(draft.id)).thenReturn(Optional.of(draft))

        assertThatThrownBy { service.fulfillSalesOrder(draft.id, null, orgId, userId) }
            .isInstanceOf(BusinessRuleException::class.java)
        verify(stockMovementService, never()).createMovement(any(), any(), any())
    }

    @Test
    fun `partial fulfill leaves the order PARTIALLY_FULFILLED`() {
        val approved = service.createSalesOrder(createRequest(), orgId, userId).copy(status = SalesOrderStatus.APPROVED)
        val lineId = approved.lines.first().id
        whenever(repository.findById(approved.id)).thenReturn(Optional.of(approved))

        val result =
            service.fulfillSalesOrder(
                approved.id,
                FulfillSalesOrderRequest(listOf(FulfillSalesOrderLine(lineId, BigDecimal("1")))),
                orgId,
                userId,
            )

        assertThat(result.status).isEqualTo(SalesOrderStatus.PARTIALLY_FULFILLED)
        assertThat(result.lines.first().fulfilledQuantity).isEqualByComparingTo("1")
    }

    @Test
    fun `generateInvoice invoices fulfilled quantity to the customer revenue account`() {
        whenever(customerService.getCustomer("c-1", orgId))
            .thenReturn(Customer(id = "c-1", name = "Buyer", organizationId = orgId, defaultRevenueAccountId = "rev-4000"))
        val fulfilled =
            service
                .createSalesOrder(createRequest(), orgId, userId)
                .let { so ->
                    so.copy(status = SalesOrderStatus.FULFILLED, lines = so.lines.map { it.copy(fulfilledQuantity = it.quantity) })
                }
        whenever(repository.findById(fulfilled.id)).thenReturn(Optional.of(fulfilled))
        whenever(invoiceService.createInvoice(any(), eq(orgId), any())).thenReturn(mock(Invoice::class.java))

        service.generateInvoice(fulfilled.id, null, orgId, userId)

        val captor = argumentCaptor<CreateInvoiceRequest>()
        verify(invoiceService).createInvoice(captor.capture(), eq(orgId), any())
        val req = captor.firstValue
        assertThat(req.lines).hasSize(1)
        assertThat(req.lines.first().accountId).isEqualTo("rev-4000")
        assertThat(req.lines.first().amount).isEqualByComparingTo("36")
    }

    @Test
    fun `generateInvoice fails when customer has no default revenue account`() {
        val fulfilled =
            service
                .createSalesOrder(createRequest(), orgId, userId)
                .let { so ->
                    so.copy(status = SalesOrderStatus.FULFILLED, lines = so.lines.map { it.copy(fulfilledQuantity = it.quantity) })
                }
        whenever(repository.findById(fulfilled.id)).thenReturn(Optional.of(fulfilled))

        assertThatThrownBy { service.generateInvoice(fulfilled.id, null, orgId, userId) }
            .isInstanceOf(BusinessRuleException::class.java)
        verify(invoiceService, never()).createInvoice(any(), any(), any())
    }
}
