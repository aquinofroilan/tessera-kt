package com.aquinofroilan.tessera.domain.sales.service

import com.aquinofroilan.tessera.domain.finance.model.Invoice
import com.aquinofroilan.tessera.domain.finance.model.InvoiceStatus
import com.aquinofroilan.tessera.domain.finance.repository.InvoiceRepository
import com.aquinofroilan.tessera.domain.sales.dto.ApplyCreditNoteRequest
import com.aquinofroilan.tessera.domain.sales.dto.CreateCreditNoteLineRequest
import com.aquinofroilan.tessera.domain.sales.dto.CreateCreditNoteRequest
import com.aquinofroilan.tessera.domain.sales.model.CreditNote
import com.aquinofroilan.tessera.domain.sales.model.CreditNoteLine
import com.aquinofroilan.tessera.domain.sales.model.CreditNoteStatus
import com.aquinofroilan.tessera.domain.sales.model.Customer
import com.aquinofroilan.tessera.domain.sales.repository.CreditNoteAllocationRepository
import com.aquinofroilan.tessera.domain.sales.repository.CreditNoteRepository
import com.aquinofroilan.tessera.domain.sales.repository.CustomerRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

class CreditNoteServiceTest {
    private lateinit var creditNoteRepository: CreditNoteRepository
    private lateinit var creditNoteAllocationRepository: CreditNoteAllocationRepository
    private lateinit var customerRepository: CustomerRepository
    private lateinit var invoiceRepository: InvoiceRepository
    private lateinit var service: CreditNoteService

    private val orgId = UUID.fromString("4abe9f6d-6df3-6e5c-953e-3695db9a5216")
    private val userId = UUID.fromString("bc17c97c-3d89-7d43-b7e0-7ca0266eafa8")
    private val customerId = UUID.fromString("11111111-2222-3333-4444-555555555555")
    private val creditNoteId = UUID.fromString("22222222-3333-4444-5555-666666666666")
    private val invoiceId = UUID.fromString("33333333-4444-5555-6666-777777777777")

    @BeforeEach
    fun setUp() {
        creditNoteRepository = mock(CreditNoteRepository::class.java)
        creditNoteAllocationRepository = mock(CreditNoteAllocationRepository::class.java)
        customerRepository = mock(CustomerRepository::class.java)
        invoiceRepository = mock(InvoiceRepository::class.java)
        service =
            CreditNoteService(
                creditNoteRepository,
                creditNoteAllocationRepository,
                customerRepository,
                invoiceRepository,
            )
    }

    private fun createCustomer() =
        Customer(
            id = customerId,
            name = "Acme Corp",
            organizationId = orgId,
        )

    private fun createCreditNote(status: CreditNoteStatus = CreditNoteStatus.APPROVED): CreditNote {
        val line =
            CreditNoteLine(
                creditNoteId = creditNoteId,
                lineNumber = 1,
                description = "Returned item credit",
                quantity = BigDecimal.ONE,
                unitPrice = BigDecimal("100.00"),
                lineTotal = BigDecimal("100.00"),
            )
        return CreditNote(
            id = creditNoteId,
            organizationId = orgId,
            creditNoteNumber = "CN-00001",
            customerId = customerId,
            customerName = "Acme Corp",
            totalAmount = BigDecimal("100.00"),
            allocatedAmount = BigDecimal.ZERO,
            status = status,
            createdBy = userId,
            lines = mutableListOf(line),
        )
    }

    private fun createInvoice(
        totalAmount: BigDecimal = BigDecimal("150.00"),
        amountReceived: BigDecimal = BigDecimal.ZERO,
    ) = Invoice(
        id = invoiceId,
        invoiceNumber = "INV-00001",
        customerId = customerId,
        customerName = "Acme Corp",
        date = LocalDate.now(),
        dueDate = LocalDate.now().plusDays(30),
        organizationId = orgId,
        status = InvoiceStatus.APPROVED,
        lines = emptyList(),
        totalAmount = totalAmount,
        amountReceived = amountReceived,
        createdBy = userId,
    )

    @Test
    fun `createCreditNote creates draft credit note`() {
        val request =
            CreateCreditNoteRequest(
                customerId = customerId,
                lines =
                    listOf(
                        CreateCreditNoteLineRequest(
                            description = "Goodwill credit",
                            unitPrice = BigDecimal("50.00"),
                        ),
                    ),
            )

        `when`(customerRepository.findByIdAndOrganizationId(customerId, orgId)).thenReturn(Optional.of(createCustomer()))
        `when`(creditNoteRepository.countByOrganizationId(orgId)).thenReturn(0L)
        `when`(creditNoteRepository.save(any<CreditNote>())).thenAnswer { it.arguments[0] }

        val response = service.createCreditNote(orgId, userId, request)

        assertEquals("CN-00001", response.creditNoteNumber)
        assertEquals(CreditNoteStatus.DRAFT, response.status)
        assertEquals(BigDecimal("50.00"), response.totalAmount)
        assertEquals(BigDecimal("50.00"), response.unallocatedAmount)
    }

    @Test
    fun `applyCreditNoteToInvoice allocates credit and updates invoice and credit note status`() {
        val cn = createCreditNote(CreditNoteStatus.APPROVED)
        val invoice = createInvoice(totalAmount = BigDecimal("100.00"))

        `when`(creditNoteRepository.findByIdAndOrganizationId(creditNoteId, orgId)).thenReturn(Optional.of(cn))
        `when`(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice))
        `when`(creditNoteRepository.save(any<CreditNote>())).thenAnswer { it.arguments[0] }

        val request =
            ApplyCreditNoteRequest(
                invoiceId = invoiceId,
                amount = BigDecimal("100.00"),
            )

        val response = service.applyCreditNoteToInvoice(creditNoteId, orgId, userId, request)

        assertEquals(CreditNoteStatus.APPLIED, response.status)
        assertEquals(0, response.allocatedAmount.compareTo(BigDecimal("100.00")))
        assertEquals(0, response.unallocatedAmount.compareTo(BigDecimal.ZERO))
        assertEquals(InvoiceStatus.PAID, invoice.status)
        assertEquals(0, invoice.amountReceived.compareTo(BigDecimal("100.00")))
        verify(creditNoteAllocationRepository).save(any())
        verify(invoiceRepository).save(invoice)
    }

    @Test
    fun `applyCreditNoteToInvoice throws error when amount exceeds unallocated credit`() {
        val cn = createCreditNote(CreditNoteStatus.APPROVED)
        val invoice = createInvoice(totalAmount = BigDecimal("150.00"))

        `when`(creditNoteRepository.findByIdAndOrganizationId(creditNoteId, orgId)).thenReturn(Optional.of(cn))
        `when`(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice))

        val request =
            ApplyCreditNoteRequest(
                invoiceId = invoiceId,
                amount = BigDecimal("120.00"),
            )

        assertThrows<BusinessRuleException> {
            service.applyCreditNoteToInvoice(creditNoteId, orgId, userId, request)
        }
    }
}
