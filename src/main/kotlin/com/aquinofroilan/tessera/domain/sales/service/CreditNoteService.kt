package com.aquinofroilan.tessera.domain.sales.service

import com.aquinofroilan.tessera.domain.finance.model.InvoiceStatus
import com.aquinofroilan.tessera.domain.finance.repository.InvoiceRepository
import com.aquinofroilan.tessera.domain.sales.dto.ApplyCreditNoteRequest
import com.aquinofroilan.tessera.domain.sales.dto.CreateCreditNoteRequest
import com.aquinofroilan.tessera.domain.sales.dto.CreditNoteResponse
import com.aquinofroilan.tessera.domain.sales.model.CreditNote
import com.aquinofroilan.tessera.domain.sales.model.CreditNoteAllocation
import com.aquinofroilan.tessera.domain.sales.model.CreditNoteLine
import com.aquinofroilan.tessera.domain.sales.model.CreditNoteStatus
import com.aquinofroilan.tessera.domain.sales.repository.CreditNoteAllocationRepository
import com.aquinofroilan.tessera.domain.sales.repository.CreditNoteRepository
import com.aquinofroilan.tessera.domain.sales.repository.CustomerRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Locale
import java.util.UUID

@Service
class CreditNoteService(
    private val creditNoteRepository: CreditNoteRepository,
    private val creditNoteAllocationRepository: CreditNoteAllocationRepository,
    private val customerRepository: CustomerRepository,
    private val invoiceRepository: InvoiceRepository,
) {
    @Transactional(readOnly = true)
    fun listCreditNotes(
        organizationId: UUID,
        customerId: UUID? = null,
        status: CreditNoteStatus? = null,
    ): List<CreditNoteResponse> {
        val notes =
            when {
                customerId != null -> creditNoteRepository.findByOrganizationIdAndCustomerId(organizationId, customerId)
                status != null -> creditNoteRepository.findByOrganizationIdAndStatus(organizationId, status)
                else -> creditNoteRepository.findByOrganizationId(organizationId)
            }
        return notes.map { CreditNoteResponse.from(it) }
    }

    @Transactional(readOnly = true)
    fun getCreditNote(
        id: UUID,
        organizationId: UUID,
    ): CreditNoteResponse {
        val note =
            creditNoteRepository.findByIdAndOrganizationId(id, organizationId).orElseThrow {
                ResourceNotFoundException("Credit note $id not found")
            }
        return CreditNoteResponse.from(note)
    }

    @Transactional
    fun createCreditNote(
        organizationId: UUID,
        userId: UUID,
        request: CreateCreditNoteRequest,
    ): CreditNoteResponse {
        val customer =
            customerRepository.findByIdAndOrganizationId(request.customerId, organizationId).orElseThrow {
                ResourceNotFoundException("Customer ${request.customerId} not found")
            }

        val count = creditNoteRepository.countByOrganizationId(organizationId)
        val creditNoteNumber = "CN-%05d".format(count + 1)
        val currency = (request.currency ?: "USD").trim().uppercase(Locale.ROOT)

        var totalAmount = BigDecimal.ZERO
        val creditNote =
            CreditNote(
                organizationId = organizationId,
                creditNoteNumber = creditNoteNumber,
                customerId = customer.id,
                customerName = customer.name,
                salesReturnId = request.salesReturnId,
                invoiceId = request.invoiceId,
                date = request.date ?: LocalDate.now(),
                currency = currency,
                totalAmount = BigDecimal.ZERO,
                status = CreditNoteStatus.DRAFT,
                reason = request.reason?.trim(),
                createdBy = userId,
            )

        request.lines.forEachIndexed { index, lineReq ->
            val qty = lineReq.quantity ?: BigDecimal.ONE
            val lineTotal = lineReq.unitPrice.multiply(qty)
            totalAmount = totalAmount.add(lineTotal)

            creditNote.lines.add(
                CreditNoteLine(
                    creditNoteId = creditNote.id,
                    lineNumber = index + 1,
                    productId = lineReq.productId,
                    description = lineReq.description.trim(),
                    quantity = qty,
                    unitPrice = lineReq.unitPrice,
                    lineTotal = lineTotal,
                    accountId = lineReq.accountId,
                ),
            )
        }

        creditNote.totalAmount = totalAmount
        val saved = creditNoteRepository.save(creditNote)
        return CreditNoteResponse.from(saved)
    }

    @Transactional
    fun approveCreditNote(
        id: UUID,
        organizationId: UUID,
        userId: UUID,
    ): CreditNoteResponse {
        val note =
            creditNoteRepository.findByIdAndOrganizationId(id, organizationId).orElseThrow {
                ResourceNotFoundException("Credit note $id not found")
            }

        if (note.status != CreditNoteStatus.DRAFT) {
            throw BusinessRuleException("Only DRAFT credit notes can be approved (current: ${note.status})")
        }

        note.status = CreditNoteStatus.APPROVED
        note.approvedBy = userId
        note.approvedAt = LocalDateTime.now(ZoneOffset.UTC)

        val saved = creditNoteRepository.save(note)
        return CreditNoteResponse.from(saved)
    }

    @Transactional
    fun applyCreditNoteToInvoice(
        creditNoteId: UUID,
        organizationId: UUID,
        userId: UUID,
        request: ApplyCreditNoteRequest,
    ): CreditNoteResponse {
        val creditNote =
            creditNoteRepository.findByIdAndOrganizationId(creditNoteId, organizationId).orElseThrow {
                ResourceNotFoundException("Credit note $creditNoteId not found")
            }

        if (creditNote.status != CreditNoteStatus.APPROVED && creditNote.status != CreditNoteStatus.PARTIALLY_APPLIED) {
            throw BusinessRuleException("Credit note is not in an applicable status (current: ${creditNote.status})")
        }

        val invoice =
            invoiceRepository.findById(request.invoiceId).orElseThrow {
                ResourceNotFoundException("Invoice ${request.invoiceId} not found")
            }
        if (invoice.organizationId != organizationId) {
            throw ResourceNotFoundException("Invoice ${request.invoiceId} not found")
        }

        if (invoice.customerId != creditNote.customerId) {
            throw BusinessRuleException("Invoice belongs to a different customer than the credit note")
        }

        if (invoice.status != InvoiceStatus.APPROVED && invoice.status != InvoiceStatus.PARTIALLY_PAID) {
            throw BusinessRuleException("Invoice cannot receive credit allocations in status ${invoice.status}")
        }

        val unallocatedCredit = creditNote.totalAmount.subtract(creditNote.allocatedAmount)
        if (request.amount > unallocatedCredit) {
            throw BusinessRuleException(
                "Requested allocation amount (${request.amount}) exceeds available credit ($unallocatedCredit)",
            )
        }

        val invoiceRemainingBalance = invoice.totalAmount.subtract(invoice.amountReceived)
        if (request.amount > invoiceRemainingBalance) {
            throw BusinessRuleException(
                "Requested allocation amount (${request.amount}) exceeds outstanding invoice balance ($invoiceRemainingBalance)",
            )
        }

        // Apply allocation to credit note
        creditNote.allocatedAmount = creditNote.allocatedAmount.add(request.amount)
        if (creditNote.allocatedAmount.compareTo(creditNote.totalAmount) >= 0) {
            creditNote.status = CreditNoteStatus.APPLIED
        } else {
            creditNote.status = CreditNoteStatus.PARTIALLY_APPLIED
        }

        val allocation =
            CreditNoteAllocation(
                organizationId = organizationId,
                creditNoteId = creditNote.id,
                invoiceId = invoice.id,
                amountApplied = request.amount,
                appliedDate = request.appliedDate ?: LocalDate.now(),
                appliedBy = userId,
            )
        creditNote.allocations.add(allocation)
        creditNoteAllocationRepository.save(allocation)

        // Apply allocation to invoice
        invoice.amountReceived = invoice.amountReceived.add(request.amount)
        invoice.baseCurrencyAmountReceived = invoice.amountReceived.multiply(invoice.exchangeRate)

        if (invoice.amountReceived.compareTo(invoice.totalAmount) >= 0) {
            invoice.status = InvoiceStatus.PAID
            invoice.paidAt = LocalDateTime.now(ZoneOffset.UTC)
        } else {
            invoice.status = InvoiceStatus.PARTIALLY_PAID
        }

        invoiceRepository.save(invoice)
        val saved = creditNoteRepository.save(creditNote)
        return CreditNoteResponse.from(saved)
    }

    @Transactional
    fun voidCreditNote(
        id: UUID,
        organizationId: UUID,
    ): CreditNoteResponse {
        val note =
            creditNoteRepository.findByIdAndOrganizationId(id, organizationId).orElseThrow {
                ResourceNotFoundException("Credit note $id not found")
            }

        if (note.allocatedAmount.compareTo(BigDecimal.ZERO) > 0) {
            throw BusinessRuleException("Cannot void a credit note that has already been partially or fully allocated")
        }

        if (note.status == CreditNoteStatus.VOID) {
            throw BusinessRuleException("Credit note is already void")
        }

        note.status = CreditNoteStatus.VOID
        val saved = creditNoteRepository.save(note)
        return CreditNoteResponse.from(saved)
    }
}
