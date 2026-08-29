package com.aquinofroilan.tessera.domain.sales.dto

import com.aquinofroilan.tessera.domain.sales.model.CreditNote
import com.aquinofroilan.tessera.domain.sales.model.CreditNoteAllocation
import com.aquinofroilan.tessera.domain.sales.model.CreditNoteLine
import com.aquinofroilan.tessera.domain.sales.model.CreditNoteStatus
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class CreditNoteLineDto(
    val id: UUID,
    val lineNumber: Int,
    val productId: UUID?,
    val description: String,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val lineTotal: BigDecimal,
    val accountId: UUID?,
) {
    companion object {
        fun from(line: CreditNoteLine): CreditNoteLineDto =
            CreditNoteLineDto(
                id = line.id,
                lineNumber = line.lineNumber,
                productId = line.productId,
                description = line.description,
                quantity = line.quantity,
                unitPrice = line.unitPrice,
                lineTotal = line.lineTotal,
                accountId = line.accountId,
            )
    }
}

data class CreditNoteAllocationDto(
    val id: UUID,
    val creditNoteId: UUID,
    val invoiceId: UUID,
    val amountApplied: BigDecimal,
    val appliedDate: LocalDate,
    val appliedBy: UUID,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(allocation: CreditNoteAllocation): CreditNoteAllocationDto =
            CreditNoteAllocationDto(
                id = allocation.id,
                creditNoteId = allocation.creditNoteId,
                invoiceId = allocation.invoiceId,
                amountApplied = allocation.amountApplied,
                appliedDate = allocation.appliedDate,
                appliedBy = allocation.appliedBy,
                createdAt = allocation.createdAt,
            )
    }
}

data class CreditNoteResponse(
    val id: UUID,
    val organizationId: UUID,
    val creditNoteNumber: String,
    val customerId: UUID,
    val customerName: String,
    val salesReturnId: UUID?,
    val invoiceId: UUID?,
    val date: LocalDate,
    val currency: String,
    val totalAmount: BigDecimal,
    val allocatedAmount: BigDecimal,
    val unallocatedAmount: BigDecimal,
    val status: CreditNoteStatus,
    val reason: String?,
    val createdBy: UUID,
    val approvedBy: UUID?,
    val approvedAt: LocalDateTime?,
    val lines: List<CreditNoteLineDto>,
    val allocations: List<CreditNoteAllocationDto>,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
) {
    companion object {
        fun from(creditNote: CreditNote): CreditNoteResponse =
            CreditNoteResponse(
                id = creditNote.id,
                organizationId = creditNote.organizationId,
                creditNoteNumber = creditNote.creditNoteNumber,
                customerId = creditNote.customerId,
                customerName = creditNote.customerName,
                salesReturnId = creditNote.salesReturnId,
                invoiceId = creditNote.invoiceId,
                date = creditNote.date,
                currency = creditNote.currency,
                totalAmount = creditNote.totalAmount,
                allocatedAmount = creditNote.allocatedAmount,
                unallocatedAmount = (creditNote.totalAmount.subtract(creditNote.allocatedAmount)).max(BigDecimal.ZERO),
                status = creditNote.status,
                reason = creditNote.reason,
                createdBy = creditNote.createdBy,
                approvedBy = creditNote.approvedBy,
                approvedAt = creditNote.approvedAt,
                lines = creditNote.lines.map { CreditNoteLineDto.from(it) },
                allocations = creditNote.allocations.map { CreditNoteAllocationDto.from(it) },
                createdAt = creditNote.createdAt,
                updatedAt = creditNote.updatedAt,
            )
    }
}

data class CreateCreditNoteLineRequest(
    val productId: UUID? = null,
    @field:NotBlank(message = "Description is required")
    val description: String,
    @field:DecimalMin(value = "0.0001", message = "Quantity must be greater than zero")
    val quantity: BigDecimal? = null,
    @field:NotNull(message = "Unit price is required")
    @field:DecimalMin(value = "0.0", message = "Unit price cannot be negative")
    val unitPrice: BigDecimal,
    val accountId: UUID? = null,
)

data class CreateCreditNoteRequest(
    @field:NotNull(message = "Customer ID is required")
    val customerId: UUID,
    val salesReturnId: UUID? = null,
    val invoiceId: UUID? = null,
    val date: LocalDate? = null,
    val currency: String? = null,
    val reason: String? = null,
    @field:NotEmpty(message = "Lines cannot be empty")
    val lines: List<CreateCreditNoteLineRequest>,
)

data class ApplyCreditNoteRequest(
    @field:NotNull(message = "Invoice ID is required")
    val invoiceId: UUID,
    @field:NotNull(message = "Amount is required")
    @field:DecimalMin(value = "0.01", message = "Applied amount must be greater than zero")
    val amount: BigDecimal,
    val appliedDate: LocalDate? = null,
)
