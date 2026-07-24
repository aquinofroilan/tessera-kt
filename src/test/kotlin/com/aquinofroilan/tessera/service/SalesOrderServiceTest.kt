package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateInvoiceRequest
import com.aquinofroilan.tessera.dto.CreateSalesOrderLineRequest
import com.aquinofroilan.tessera.dto.CreateSalesOrderRequest
import com.aquinofroilan.tessera.dto.FulfillSalesOrderLine
import com.aquinofroilan.tessera.dto.FulfillSalesOrderRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.model.Customer
import com.aquinofroilan.tessera.model.Invoice
import com.aquinofroilan.tessera.model.Product
import com.aquinofroilan.tessera.model.SalesOrder
import com.aquinofroilan.tessera.model.SalesOrderStatus
import com.aquinofroilan.tessera.model.StockMovementType
import com.aquinofroilan.tessera.model.Warehouse
import com.aquinofroilan.tessera.repository.SalesOrderRepository
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

    private val orgId = java.util.UUID.fromString("e5628ca4-87a8-3e6f-8ae2-20213cc7ef92")
    private val userId = java.util.UUID.fromString("1db2395f-13ba-3d37-9d2b-f77d3eb3aa2e")

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
        whenever(customerService.getCustomer(java.util.UUID.fromString("3fee4eba-8bd0-3b22-b897-e836ed3ce230"), orgId)).thenReturn(Customer(id = java.util.UUID.fromString("3fee4eba-8bd0-3b22-b897-e836ed3ce230"), name = "Buyer", organizationId = orgId))
        whenever(warehouseService.getWarehouse(java.util.UUID.fromString("c91d2c12-b2b4-3634-a3bb-d0ff561af4ff"), orgId))
            .thenReturn(Warehouse(id = java.util.UUID.fromString("c91d2c12-b2b4-3634-a3bb-d0ff561af4ff"), code = "MAIN", name = "Main", organizationId = orgId))
        whenever(productService.getProduct(java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89"), orgId)).thenReturn(
            Product(id = java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89"), sku = "SKU-1", name = "Widget", listPrice = BigDecimal("9"), priceCurrency = "USD", organizationId = orgId),
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
            customerId = java.util.UUID.fromString("3fee4eba-8bd0-3b22-b897-e836ed3ce230"),
            warehouseId = java.util.UUID.fromString("c91d2c12-b2b4-3634-a3bb-d0ff561af4ff"),
            orderDate = LocalDate.of(2026, 5, 1),
            lines = listOf(CreateSalesOrderLineRequest(productId = java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89"), quantity = BigDecimal("4"), unitPrice = BigDecimal("9"))),
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
        val approved = service.createSalesOrder(createRequest(), orgId, userId).apply { status = SalesOrderStatus.APPROVED }
        whenever(repository.findById(approved.id)).thenReturn(Optional.of(approved))

        val fulfilled = service.fulfillSalesOrder(approved.id, null, orgId, userId)

        assertThat(fulfilled.status).isEqualTo(SalesOrderStatus.FULFILLED)
        verify(stockMovementService, times(1)).createMovement(
            argThat { type == StockMovementType.ISSUE && warehouseId == java.util.UUID.fromString("c91d2c12-b2b4-3634-a3bb-d0ff561af4ff") && productId == java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89") },
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
        val approved = service.createSalesOrder(createRequest(), orgId, userId).apply { status = SalesOrderStatus.APPROVED }
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
        whenever(customerService.getCustomer(java.util.UUID.fromString("3fee4eba-8bd0-3b22-b897-e836ed3ce230"), orgId))
            .thenReturn(Customer(id = java.util.UUID.fromString("3fee4eba-8bd0-3b22-b897-e836ed3ce230"), name = "Buyer", organizationId = orgId, defaultRevenueAccountId = java.util.UUID.fromString("fa05fbc9-4ba3-3224-970e-34049bab227a")))
        val fulfilled =
            service
                .createSalesOrder(createRequest(), orgId, userId)
                .let { so ->
                    so.apply {
                        status = SalesOrderStatus.FULFILLED
                        lines = lines.map { line -> line.apply { fulfilledQuantity = quantity } }
                    }
                }
        whenever(repository.findById(fulfilled.id)).thenReturn(Optional.of(fulfilled))
        whenever(invoiceService.createInvoice(any(), eq(orgId), any())).thenReturn(mock(Invoice::class.java))

        service.generateInvoice(fulfilled.id, null, orgId, userId)

        val captor = argumentCaptor<CreateInvoiceRequest>()
        verify(invoiceService).createInvoice(captor.capture(), eq(orgId), any())
        val req = captor.firstValue
        assertThat(req.lines).hasSize(1)
        assertThat(req.lines.first().accountId).isEqualTo(java.util.UUID.fromString("fa05fbc9-4ba3-3224-970e-34049bab227a"))
        assertThat(req.lines.first().amount).isEqualByComparingTo("36")
    }

    @Test
    fun `generateInvoice fails when customer has no default revenue account`() {
        val fulfilled =
            service
                .createSalesOrder(createRequest(), orgId, userId)
                .let { so ->
                    so.apply {
                        status = SalesOrderStatus.FULFILLED
                        lines = lines.map { line -> line.apply { fulfilledQuantity = quantity } }
                    }
                }
        whenever(repository.findById(fulfilled.id)).thenReturn(Optional.of(fulfilled))

        assertThatThrownBy { service.generateInvoice(fulfilled.id, null, orgId, userId) }
            .isInstanceOf(BusinessRuleException::class.java)
        verify(invoiceService, never()).createInvoice(any(), any(), any())
    }
}
