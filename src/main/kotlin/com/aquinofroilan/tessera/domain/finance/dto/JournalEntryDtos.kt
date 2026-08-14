package com.aquinofroilan.tessera.domain.finance.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDate

data class JournalEntryLineRequest(
    @field:NotNull(message = "Account ID is required")
    val accountId: java.util.UUID,
    val debit: BigDecimal = BigDecimal.ZERO,
    val credit: BigDecimal = BigDecimal.ZERO,
    val description: String? = null,
)

data class CreateJournalEntryRequest(
    val date: LocalDate,
    @field:NotBlank(message = "Description is required")
    val description: String,
    @field:NotEmpty(message = "At least one line item is required")
    @field:Valid
    val lines: List<JournalEntryLineRequest>,
    val sourceReference: String? = null,
)

data class VoidJournalEntryRequest(
    @field:NotBlank(message = "Void reason is required")
    val reason: String,
)

data class JournalEntryLineResponse(
    val accountId: java.util.UUID,
    val accountCode: String,
    val accountName: String,
    val debit: BigDecimal,
    val credit: BigDecimal,
    val description: String?,
)

data class JournalEntryResponse(
    val id: java.util.UUID,
    val entryNumber: String,
    val date: String,
    val description: String,
    val organizationId: java.util.UUID,
    val status: String,
    val source: String,
    val sourceReference: String?,
    val lines: List<JournalEntryLineResponse>,
    val createdBy: java.util.UUID,
    val postedAt: String?,
    val voidedAt: String?,
    val voidReason: String?,
    val createdAt: String?,
    val updatedAt: String?,
)

data class AccountBalanceResponse(
    val accountId: java.util.UUID,
    val accountCode: String,
    val accountName: String,
    val accountType: String,
    val totalDebits: BigDecimal,
    val totalCredits: BigDecimal,
    val balance: BigDecimal,
)

data class TrialBalanceResponse(
    val accounts: List<AccountBalanceResponse>,
    val totalDebits: BigDecimal,
    val totalCredits: BigDecimal,
    val asOfDate: String?,
)
