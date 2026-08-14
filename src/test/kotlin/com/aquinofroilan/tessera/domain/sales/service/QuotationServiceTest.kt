package com.aquinofroilan.tessera.domain.sales.service

import com.aquinofroilan.tessera.domain.inventory.model.Product
import com.aquinofroilan.tessera.domain.inventory.model.Warehouse
import com.aquinofroilan.tessera.domain.inventory.service.ProductService
import com.aquinofroilan.tessera.domain.inventory.service.WarehouseService
import com.aquinofroilan.tessera.domain.sales.dto.ConvertQuotationRequest
import com.aquinofroilan.tessera.domain.sales.dto.CreateQuotationLineRequest
import com.aquinofroilan.tessera.domain.sales.dto.CreateQuotationRequest
import com.aquinofroilan.tessera.domain.sales.dto.CreateSalesOrderRequest
import com.aquinofroilan.tessera.domain.sales.model.Customer
import com.aquinofroilan.tessera.domain.sales.model.Quotation
import com.aquinofroilan.tessera.domain.sales.model.QuotationLine
import com.aquinofroilan.tessera.domain.sales.model.QuotationStatus
import com.aquinofroilan.tessera.domain.sales.model.SalesOrder
import com.aquinofroilan.tessera.domain.sales.repository.QuotationRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
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
import java.util.UUID

class QuotationServiceTest {
    private lateinit var repository: QuotationRepository
    private lateinit var customerService: CustomerService
    private lateinit var warehouseService: WarehouseService
    private lateinit var productService: ProductService
    private lateinit var salesOrderService: SalesOrderService
    private lateinit var service: QuotationService

    private val orgId = java.util.UUID.fromString("e5628ca4-87a8-3e6f-8ae2-20213cc7ef92")
    private val userId = java.util.UUID.fromString("1db2395f-13ba-3d37-9d2b-f77d3eb3aa2e")

    @BeforeEach
    fun setup() {
        repository = mock(QuotationRepository::class.java)
        customerService = mock(CustomerService::class.java)
        warehouseService = mock(WarehouseService::class.java)
        productService = mock(ProductService::class.java)
        salesOrderService = mock(SalesOrderService::class.java)
        whenever(repository.countByOrganizationId(orgId)).thenReturn(0L)
        whenever(repository.save(any<Quotation>())).thenAnswer { it.arguments[0] }
        whenever(
            customerService.getCustomer(java.util.UUID.fromString("3fee4eba-8bd0-3b22-b897-e836ed3ce230"), orgId),
        ).thenReturn(
            Customer(id = java.util.UUID.fromString("3fee4eba-8bd0-3b22-b897-e836ed3ce230"), name = "Globex", organizationId = orgId),
        )
        whenever(warehouseService.getWarehouse(java.util.UUID.fromString("c91d2c12-b2b4-3634-a3bb-d0ff561af4ff"), orgId))
            .thenReturn(
                Warehouse(
                    id = java.util.UUID.fromString("c91d2c12-b2b4-3634-a3bb-d0ff561af4ff"),
                    code = "MAIN",
                    name = "Main",
                    organizationId = orgId,
                ),
            )
        whenever(productService.getProduct(java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89"), orgId)).thenReturn(
            Product(
                id = java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89"),
                sku = "SKU-1",
                name = "Widget",
                listPrice = BigDecimal("9"),
                priceCurrency = "USD",
                organizationId = orgId,
            ),
        )
        service = QuotationService(repository, customerService, warehouseService, productService, salesOrderService)
    }

    private fun lineReq() =
        CreateQuotationLineRequest(
            productId = java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89"),
            quantity = BigDecimal("2"),
            unitPrice = BigDecimal("10"),
        )

    private fun acceptedQuote(
        warehouseId: UUID? = java.util.UUID.fromString("c91d2c12-b2b4-3634-a3bb-d0ff561af4ff"),
        validUntil: LocalDate? = null,
        status: QuotationStatus = QuotationStatus.ACCEPTED,
    ) = Quotation(
        id = java.util.UUID.fromString("10ac9264-b864-3f2b-872a-26d40a9b3b37"),
        quoteNumber = "QT-0001",
        customerId = java.util.UUID.fromString("3fee4eba-8bd0-3b22-b897-e836ed3ce230"),
        customerName = "Globex",
        warehouseId = warehouseId,
        quoteDate = LocalDate.of(2026, 5, 1),
        validUntil = validUntil,
        organizationId = orgId,
        status = status,
        lines =
            listOf(
                QuotationLine(
                    id = java.util.UUID.fromString("837c38e2-9ccb-3ecc-afb8-b21e3b6e6717"),
                    lineNumber = 1,
                    productId = java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89"),
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
                    customerId = java.util.UUID.fromString("3fee4eba-8bd0-3b22-b897-e836ed3ce230"),
                    warehouseId = java.util.UUID.fromString("c91d2c12-b2b4-3634-a3bb-d0ff561af4ff"),
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
        whenever(
            repository.findById(java.util.UUID.fromString("10ac9264-b864-3f2b-872a-26d40a9b3b37")),
        ).thenReturn(Optional.of(acceptedQuote(status = QuotationStatus.DRAFT)))
        assertThat(
            service.sendQuotation(java.util.UUID.fromString("10ac9264-b864-3f2b-872a-26d40a9b3b37"), orgId).status,
        ).isEqualTo(QuotationStatus.SENT)
    }

    @Test
    fun `accept rejects a quote that is not sent`() {
        whenever(
            repository.findById(java.util.UUID.fromString("10ac9264-b864-3f2b-872a-26d40a9b3b37")),
        ).thenReturn(Optional.of(acceptedQuote()))
        assertThatThrownBy { service.acceptQuotation(java.util.UUID.fromString("10ac9264-b864-3f2b-872a-26d40a9b3b37"), orgId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `accept rejects an expired quote`() {
        whenever(repository.findById(java.util.UUID.fromString("10ac9264-b864-3f2b-872a-26d40a9b3b37")))
            .thenReturn(Optional.of(acceptedQuote(status = QuotationStatus.SENT, validUntil = LocalDate.of(2000, 1, 1))))
        assertThatThrownBy { service.acceptQuotation(java.util.UUID.fromString("10ac9264-b864-3f2b-872a-26d40a9b3b37"), orgId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `cancel rejects a converted quote`() {
        whenever(
            repository.findById(java.util.UUID.fromString("10ac9264-b864-3f2b-872a-26d40a9b3b37")),
        ).thenReturn(Optional.of(acceptedQuote(status = QuotationStatus.CONVERTED)))
        assertThatThrownBy { service.cancelQuotation(java.util.UUID.fromString("10ac9264-b864-3f2b-872a-26d40a9b3b37"), orgId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `convert requires an accepted quote`() {
        whenever(
            repository.findById(java.util.UUID.fromString("10ac9264-b864-3f2b-872a-26d40a9b3b37")),
        ).thenReturn(Optional.of(acceptedQuote(status = QuotationStatus.SENT)))
        assertThatThrownBy {
            service.convertToSalesOrder(
                java.util.UUID.fromString("10ac9264-b864-3f2b-872a-26d40a9b3b37"),
                ConvertQuotationRequest(),
                orgId,
                userId,
            )
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `convert builds a sales order from quote lines and marks it converted`() {
        whenever(
            repository.findById(java.util.UUID.fromString("10ac9264-b864-3f2b-872a-26d40a9b3b37")),
        ).thenReturn(Optional.of(acceptedQuote()))
        val createdSo =
            SalesOrder(
                id = java.util.UUID.fromString("8ce4a550-f369-3e9f-be38-d2cdfa73a158"),
                soNumber = "SO-0001",
                customerId = java.util.UUID.fromString("3fee4eba-8bd0-3b22-b897-e836ed3ce230"),
                customerName = "Globex",
                warehouseId = java.util.UUID.fromString("c91d2c12-b2b4-3634-a3bb-d0ff561af4ff"),
                orderDate = LocalDate.of(2026, 5, 1),
                organizationId = orgId,
                lines = emptyList(),
                totalAmount = BigDecimal("20"),
                createdBy = userId,
            )
        whenever(salesOrderService.createSalesOrder(any(), eq(orgId), eq(userId))).thenReturn(createdSo)

        val so =
            service.convertToSalesOrder(
                java.util.UUID.fromString("10ac9264-b864-3f2b-872a-26d40a9b3b37"),
                ConvertQuotationRequest(),
                orgId,
                userId,
            )

        assertThat(so.id).isEqualTo(java.util.UUID.fromString("8ce4a550-f369-3e9f-be38-d2cdfa73a158"))
        val soReq = argumentCaptor<CreateSalesOrderRequest>()
        verify(salesOrderService).createSalesOrder(soReq.capture(), eq(orgId), eq(userId))
        assertThat(soReq.firstValue.customerId).isEqualTo(java.util.UUID.fromString("3fee4eba-8bd0-3b22-b897-e836ed3ce230"))
        assertThat(soReq.firstValue.warehouseId).isEqualTo(java.util.UUID.fromString("c91d2c12-b2b4-3634-a3bb-d0ff561af4ff"))
        assertThat(soReq.firstValue.lines[0].unitPrice).isEqualByComparingTo("10")

        val saved = argumentCaptor<Quotation>()
        verify(repository).save(saved.capture())
        assertThat(saved.firstValue.status).isEqualTo(QuotationStatus.CONVERTED)
        assertThat(saved.firstValue.convertedSalesOrderId).isEqualTo(java.util.UUID.fromString("8ce4a550-f369-3e9f-be38-d2cdfa73a158"))
    }

    @Test
    fun `convert without a warehouse anywhere is rejected`() {
        whenever(
            repository.findById(java.util.UUID.fromString("10ac9264-b864-3f2b-872a-26d40a9b3b37")),
        ).thenReturn(Optional.of(acceptedQuote(warehouseId = null)))
        assertThatThrownBy {
            service.convertToSalesOrder(
                java.util.UUID.fromString("10ac9264-b864-3f2b-872a-26d40a9b3b37"),
                ConvertQuotationRequest(),
                orgId,
                userId,
            )
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `get rejects cross-org access`() {
        whenever(
            repository.findById(java.util.UUID.fromString("10ac9264-b864-3f2b-872a-26d40a9b3b37")),
        ).thenReturn(Optional.of(acceptedQuote().copy(organizationId = java.util.UUID.fromString("f022a845-ae01-3e07-ae04-7fc0ffb096a8"))))
        assertThatThrownBy { service.getQuotation(java.util.UUID.fromString("10ac9264-b864-3f2b-872a-26d40a9b3b37"), orgId) }
            .isInstanceOf(ResourceNotFoundException::class.java)
    }
}
