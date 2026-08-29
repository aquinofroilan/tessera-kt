package com.aquinofroilan.tessera.domain.sales.service

import com.aquinofroilan.tessera.domain.inventory.model.Product
import com.aquinofroilan.tessera.domain.inventory.model.Warehouse
import com.aquinofroilan.tessera.domain.inventory.repository.ProductRepository
import com.aquinofroilan.tessera.domain.inventory.repository.WarehouseRepository
import com.aquinofroilan.tessera.domain.inventory.service.StockMovementService
import com.aquinofroilan.tessera.domain.sales.dto.CreateSalesReturnLineRequest
import com.aquinofroilan.tessera.domain.sales.dto.CreateSalesReturnRequest
import com.aquinofroilan.tessera.domain.sales.dto.CreditNoteResponse
import com.aquinofroilan.tessera.domain.sales.model.CreditNoteStatus
import com.aquinofroilan.tessera.domain.sales.model.Customer
import com.aquinofroilan.tessera.domain.sales.model.ReturnReason
import com.aquinofroilan.tessera.domain.sales.model.SalesReturn
import com.aquinofroilan.tessera.domain.sales.model.SalesReturnLine
import com.aquinofroilan.tessera.domain.sales.model.SalesReturnStatus
import com.aquinofroilan.tessera.domain.sales.repository.CustomerRepository
import com.aquinofroilan.tessera.domain.sales.repository.SalesReturnRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

class SalesReturnServiceTest {
    private lateinit var salesReturnRepository: SalesReturnRepository
    private lateinit var customerRepository: CustomerRepository
    private lateinit var warehouseRepository: WarehouseRepository
    private lateinit var productRepository: ProductRepository
    private lateinit var stockMovementService: StockMovementService
    private lateinit var creditNoteService: CreditNoteService
    private lateinit var service: SalesReturnService

    private val orgId = UUID.fromString("4abe9f6d-6df3-6e5c-953e-3695db9a5216")
    private val userId = UUID.fromString("bc17c97c-3d89-7d43-b7e0-7ca0266eafa8")
    private val customerId = UUID.fromString("11111111-2222-3333-4444-555555555555")
    private val warehouseId = UUID.fromString("22222222-3333-4444-5555-666666666666")
    private val productId = UUID.fromString("33333333-4444-5555-6666-777777777777")
    private val returnId = UUID.fromString("44444444-5555-6666-7777-888888888888")

    @BeforeEach
    fun setUp() {
        salesReturnRepository = mock(SalesReturnRepository::class.java)
        customerRepository = mock(CustomerRepository::class.java)
        warehouseRepository = mock(WarehouseRepository::class.java)
        productRepository = mock(ProductRepository::class.java)
        stockMovementService = mock(StockMovementService::class.java)
        creditNoteService = mock(CreditNoteService::class.java)
        service =
            SalesReturnService(
                salesReturnRepository,
                customerRepository,
                warehouseRepository,
                productRepository,
                stockMovementService,
                creditNoteService,
            )
    }

    private fun createCustomer() =
        Customer(
            id = customerId,
            name = "Acme Corp",
            organizationId = orgId,
        )

    private fun createWarehouse() =
        Warehouse(
            id = warehouseId,
            code = "WH-MAIN",
            name = "Main Warehouse",
            organizationId = orgId,
        )

    private fun createProduct() =
        Product(
            id = productId,
            sku = "SKU-001",
            name = "Test Product",
            listPrice = BigDecimal("50.00"),
            priceCurrency = "USD",
            organizationId = orgId,
        )

    private fun createSalesReturn(status: SalesReturnStatus = SalesReturnStatus.REQUESTED): SalesReturn {
        val line =
            SalesReturnLine(
                salesReturnId = returnId,
                lineNumber = 1,
                productId = productId,
                productSku = "SKU-001",
                productName = "Test Product",
                quantity = BigDecimal("2.0"),
                unitPrice = BigDecimal("50.00"),
                lineTotal = BigDecimal("100.00"),
            )
        return SalesReturn(
            id = returnId,
            organizationId = orgId,
            returnNumber = "RMA-00001",
            customerId = customerId,
            customerName = "Acme Corp",
            warehouseId = warehouseId,
            returnDate = LocalDate.now(),
            status = status,
            reason = ReturnReason.DEFECTIVE,
            restockInventory = true,
            totalAmount = BigDecimal("100.00"),
            createdBy = userId,
            lines = mutableListOf(line),
        )
    }

    @Test
    fun `createSalesReturn creates RMA in REQUESTED status`() {
        val request =
            CreateSalesReturnRequest(
                customerId = customerId,
                warehouseId = warehouseId,
                reason = ReturnReason.DEFECTIVE,
                lines =
                    listOf(
                        CreateSalesReturnLineRequest(
                            productId = productId,
                            quantity = BigDecimal("2.0"),
                            unitPrice = BigDecimal("50.00"),
                        ),
                    ),
            )

        `when`(customerRepository.findByIdAndOrganizationId(customerId, orgId)).thenReturn(Optional.of(createCustomer()))
        `when`(warehouseRepository.findById(warehouseId)).thenReturn(Optional.of(createWarehouse()))
        `when`(productRepository.findById(productId)).thenReturn(Optional.of(createProduct()))
        `when`(salesReturnRepository.countByOrganizationId(orgId)).thenReturn(0L)
        `when`(salesReturnRepository.save(any<SalesReturn>())).thenAnswer { it.arguments[0] }

        val response = service.createSalesReturn(orgId, userId, request)

        assertEquals("RMA-00001", response.returnNumber)
        assertEquals(SalesReturnStatus.REQUESTED, response.status)
        assertEquals(0, response.totalAmount.compareTo(BigDecimal("100.00")))
        assertEquals(1, response.lines.size)
    }

    @Test
    fun `approveSalesReturn transitions to APPROVED`() {
        val ret = createSalesReturn(SalesReturnStatus.REQUESTED)
        `when`(salesReturnRepository.findByIdAndOrganizationId(returnId, orgId)).thenReturn(Optional.of(ret))
        `when`(salesReturnRepository.save(any<SalesReturn>())).thenAnswer { it.arguments[0] }

        val response = service.approveSalesReturn(returnId, orgId, userId)

        assertEquals(SalesReturnStatus.APPROVED, response.status)
        assertNotNull(response.approvedAt)
    }

    @Test
    fun `receiveSalesReturn restocks inventory when restockInventory is true`() {
        val ret = createSalesReturn(SalesReturnStatus.APPROVED)
        `when`(salesReturnRepository.findByIdAndOrganizationId(returnId, orgId)).thenReturn(Optional.of(ret))
        `when`(salesReturnRepository.save(any<SalesReturn>())).thenAnswer { it.arguments[0] }

        val response = service.receiveSalesReturn(returnId, orgId, userId)

        assertEquals(SalesReturnStatus.RECEIVED, response.status)
        verify(stockMovementService).createMovement(any(), eq(orgId), eq(userId))
    }

    @Test
    fun `completeSalesReturn issues credit note and sets COMPLETED`() {
        val ret = createSalesReturn(SalesReturnStatus.RECEIVED)
        `when`(salesReturnRepository.findByIdAndOrganizationId(returnId, orgId)).thenReturn(Optional.of(ret))
        `when`(salesReturnRepository.save(any<SalesReturn>())).thenAnswer { it.arguments[0] }

        val mockCnResponse =
            CreditNoteResponse(
                id = UUID.randomUUID(),
                organizationId = orgId,
                creditNoteNumber = "CN-00001",
                customerId = customerId,
                customerName = "Acme Corp",
                salesReturnId = returnId,
                invoiceId = null,
                date = LocalDate.now(),
                currency = "USD",
                totalAmount = BigDecimal("100.00"),
                allocatedAmount = BigDecimal.ZERO,
                unallocatedAmount = BigDecimal("100.00"),
                status = CreditNoteStatus.DRAFT,
                reason = "Credit Note",
                createdBy = userId,
                approvedBy = null,
                approvedAt = null,
                lines = emptyList(),
                allocations = emptyList(),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )
        `when`(creditNoteService.createCreditNote(eq(orgId), eq(userId), any())).thenReturn(mockCnResponse)

        val response = service.completeSalesReturn(returnId, orgId, userId, issueCreditNote = true)

        assertEquals(SalesReturnStatus.COMPLETED, response.status)
        verify(creditNoteService).createCreditNote(eq(orgId), eq(userId), any())
        verify(creditNoteService).approveCreditNote(eq(mockCnResponse.id), eq(orgId), eq(userId))
    }

    @Test
    fun `cancelSalesReturn throws exception when in RECEIVED status`() {
        val ret = createSalesReturn(SalesReturnStatus.RECEIVED)
        `when`(salesReturnRepository.findByIdAndOrganizationId(returnId, orgId)).thenReturn(Optional.of(ret))

        assertThrows<BusinessRuleException> {
            service.cancelSalesReturn(returnId, orgId)
        }
    }
}
