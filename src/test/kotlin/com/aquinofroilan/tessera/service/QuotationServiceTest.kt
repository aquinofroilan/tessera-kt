package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.ConvertQuotationRequest
import com.aquinofroilan.tessera.dto.CreateQuotationLineRequest
import com.aquinofroilan.tessera.dto.CreateQuotationRequest
import com.aquinofroilan.tessera.dto.CreateSalesOrderRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.Customer
import com.aquinofroilan.tessera.model.Product
import com.aquinofroilan.tessera.model.Quotation
import com.aquinofroilan.tessera.model.QuotationLine
import com.aquinofroilan.tessera.model.QuotationStatus
import com.aquinofroilan.tessera.model.SalesOrder
import com.aquinofroilan.tessera.model.Warehouse
import com.aquinofroilan.tessera.repository.QuotationRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

class QuotationServiceTest {
    private lateinit var repository: QuotationRepository
    private lateinit var customerService: CustomerService
    private lateinit var warehouseService: WarehouseService
    private lateinit var productService: ProductService
    private lateinit var salesOrderService: SalesOrderService
    private lateinit var service: QuotationService

    private val orgId = "org-1"
    private val userId = "user-1"

    @BeforeEach
    fun setup() {
        repository = mock(QuotationRepository::class.java)
        customerService = mock(CustomerService::class.java)
        warehouseService = mock(WarehouseService::class.java)
        productService = mock(ProductService::class.java)
        salesOrderService = mock(SalesOrderService::class.java)
        whenever(repository.countByOrganizationId(orgId)).thenReturn(0L)
        whenever(repository.save(any<Quotation>())).thenAnswer { it.arguments[0] }
        whenever(customerService.getCustomer("c-1", orgId)).thenReturn(Customer(id = "c-1", name = "Globex", organizationId = orgId))
        whenever(warehouseService.getWarehouse("wh-1", orgId))
            .thenReturn(Warehouse(id = "wh-1", code = "MAIN", name = "Main", organizationId = orgId))
        whenever(productService.getProduct("p-1", orgId)).thenReturn(
            Product(id = "p-1", sku = "SKU-1", name = "Widget", listPrice = BigDecimal("9"), priceCurrency = "USD", organizationId = orgId),
        )
        service = QuotationService(repository, customerService, warehouseService, productService, salesOrderService)
    }

    private fun lineReq() = CreateQuotationLineRequest(productId = "p-1", quantity = BigDecimal("2"), unitPrice = BigDecimal("10"))

    private fun acceptedQuote(
        warehouseId: String? = "wh-1",
        validUntil: LocalDate? = null,
        status: QuotationStatus = QuotationStatus.ACCEPTED,
    ) = Quotation(
        id = "q-1",
        quoteNumber = "QT-0001",
        customerId = "c-1",
        customerName = "Globex",
        warehouseId = warehouseId,
        quoteDate = LocalDate.of(2026, 5, 1),
        validUntil = validUntil,
        organizationId = orgId,
        status = status,
        lines =
            listOf(
                QuotationLine(
                    id = "ql-1",
                    lineNumber = 1,
                    productId = "p-1",
                    productSku = "SKU-1",
                    productName = "Widget",
                    quantity = BigDecimal("2"),
                    unitPrice = BigDecimal("10"),
                    lineTotal = BigDecimal("20"),
                ),
            ),
        totalAmount = BigDecimal("20"),
        createdBy = userId,
    )

    @Test
    fun `create persists a draft with a generated number and computed total`() {
        val quote =
            service.createQuotation(
                CreateQuotationRequest(
                    customerId = "c-1",
                    warehouseId = "wh-1",
                    quoteDate = LocalDate.of(2026, 5, 1),
                    lines = listOf(lineReq()),
                ),
                orgId,
                userId,
            )

        assertThat(quote.status).isEqualTo(QuotationStatus.DRAFT)
        assertThat(quote.quoteNumber).isEqualTo("QT-0001")
        assertThat(quote.totalAmount).isEqualByComparingTo("20")
    }

    @Test
    fun `send moves a draft to sent`() {
        whenever(repository.findById("q-1")).thenReturn(Optional.of(acceptedQuote(status = QuotationStatus.DRAFT)))
        assertThat(service.sendQuotation("q-1", orgId).status).isEqualTo(QuotationStatus.SENT)
    }

    @Test
    fun `accept rejects a quote that is not sent`() {
        whenever(repository.findById("q-1")).thenReturn(Optional.of(acceptedQuote()))
        assertThatThrownBy { service.acceptQuotation("q-1", orgId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `accept rejects an expired quote`() {
        whenever(repository.findById("q-1"))
            .thenReturn(Optional.of(acceptedQuote(status = QuotationStatus.SENT, validUntil = LocalDate.of(2000, 1, 1))))
        assertThatThrownBy { service.acceptQuotation("q-1", orgId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `cancel rejects a converted quote`() {
        whenever(repository.findById("q-1")).thenReturn(Optional.of(acceptedQuote(status = QuotationStatus.CONVERTED)))
        assertThatThrownBy { service.cancelQuotation("q-1", orgId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `convert requires an accepted quote`() {
        whenever(repository.findById("q-1")).thenReturn(Optional.of(acceptedQuote(status = QuotationStatus.SENT)))
        assertThatThrownBy { service.convertToSalesOrder("q-1", ConvertQuotationRequest(), orgId, userId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `convert builds a sales order from quote lines and marks it converted`() {
        whenever(repository.findById("q-1")).thenReturn(Optional.of(acceptedQuote()))
        val createdSo =
            SalesOrder(
                id = "so-9",
                soNumber = "SO-0001",
                customerId = "c-1",
                customerName = "Globex",
                warehouseId = "wh-1",
                orderDate = LocalDate.of(2026, 5, 1),
                organizationId = orgId,
                lines = emptyList(),
                totalAmount = BigDecimal("20"),
                createdBy = userId,
            )
        whenever(salesOrderService.createSalesOrder(any(), eq(orgId), eq(userId))).thenReturn(createdSo)

        val so = service.convertToSalesOrder("q-1", ConvertQuotationRequest(), orgId, userId)

        assertThat(so.id).isEqualTo("so-9")
        val soReq = argumentCaptor<CreateSalesOrderRequest>()
        verify(salesOrderService).createSalesOrder(soReq.capture(), eq(orgId), eq(userId))
        assertThat(soReq.firstValue.customerId).isEqualTo("c-1")
        assertThat(soReq.firstValue.warehouseId).isEqualTo("wh-1")
        assertThat(soReq.firstValue.lines[0].unitPrice).isEqualByComparingTo("10")

        val saved = argumentCaptor<Quotation>()
        verify(repository).save(saved.capture())
        assertThat(saved.firstValue.status).isEqualTo(QuotationStatus.CONVERTED)
        assertThat(saved.firstValue.convertedSalesOrderId).isEqualTo("so-9")
    }

    @Test
    fun `convert without a warehouse anywhere is rejected`() {
        whenever(repository.findById("q-1")).thenReturn(Optional.of(acceptedQuote(warehouseId = null)))
        assertThatThrownBy { service.convertToSalesOrder("q-1", ConvertQuotationRequest(), orgId, userId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `get rejects cross-org access`() {
        whenever(repository.findById("q-1")).thenReturn(Optional.of(acceptedQuote().copy(organizationId = "other")))
        assertThatThrownBy { service.getQuotation("q-1", orgId) }
            .isInstanceOf(ResourceNotFoundException::class.java)
    }
}
