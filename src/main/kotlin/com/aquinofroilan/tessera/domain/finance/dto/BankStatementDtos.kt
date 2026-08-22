package com.aquinofroilan.tessera.domain.finance.dto

import com.aquinofroilan.tessera.domain.finance.model.BankStatement
import com.aquinofroilan.tessera.domain.finance.model.BankStatementLine
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class ImportStatementLineRequest(
    @field:NotNull(message = "Posted date is required")
    val postedDate: LocalDate?,
    @field:NotBlank(message = "Description is required")
    val description: String,
    val reference: String? = null,
    @field:NotNull(message = "Amount is required")
    val amount: BigDecimal?,
)

data class ImportStatementRequest(
    @field:NotBlank(message = "Bank account ID is required")
    val bankAccountId: java.util.UUID,
    @field:NotNull(message = "Statement date is required")
    val statementDate: LocalDate?,
    @field:NotNull(message = "Opening balance is required")
    val openingBalance: BigDecimal?,
    @field:NotNull(message = "Closing balance is required")
    val closingBalance: BigDecimal?,
    val source: String? = null,
    val notes: String? = null,
    @field:NotEmpty(message = "At least one line is required")
    @field:Valid
    val lines: List<ImportStatementLineRequest>,
)

data class BankStatementLineResponse(
    val id: java.util.UUID,
    val lineNumber: Int,
    val postedDate: LocalDate,
    val description: String,
    val reference: String?,
    val amount: BigDecimal,
    val reconciled: Boolean,
    val reconciledJournalEntryId: java.util.UUID?,
    val reconciledAt: LocalDateTime?,
) {
    companion object {
        fun from(l: BankStatementLine) =
            BankStatementLineResponse(
                id = l.id,
                lineNumber = l.lineNumber,
                postedDate = l.postedDate,
                description = l.description,
                reference = l.reference,
                amount = l.amount,
                reconciled = l.reconciled,
                reconciledJournalEntryId = l.reconciledJournalEntryId,
                reconciledAt = l.reconciledAt,
            )
    }
}

data class BankStatementResponse(
    val id: java.util.UUID,
    val bankAccountId: java.util.UUID,
    val statementDate: LocalDate,
    val openingBalance: BigDecimal,
    val closingBalance: BigDecimal,
    val currency: String,
    val source: String,
    val notes: String?,
    val lines: List<BankStatementLineResponse>,
) {
    companion object {
        fun from(s: BankStatement) =
            BankStatementResponse(
                id = s.id,
                bankAccountId = s.bankAccountId,
                statementDate = s.statementDate,
                openingBalance = s.openingBalance,
                closingBalance = s.closingBalance,
                currency = s.currency,
                source = s.source,
                notes = s.notes,
                lines = s.lines.map { BankStatementLineResponse.from(it) },
            )
    }
}
