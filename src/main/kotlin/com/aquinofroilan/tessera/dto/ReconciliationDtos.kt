package com.aquinofroilan.tessera.dto

import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.time.LocalDate

data class ManualMatchRequest(
    @field:NotBlank(message = "Journal entry ID is required")
    val journalEntryId: String,
)

data class AutoMatchRequest(
    val maxDateDriftDays: Int = 5,
)

data class MatchedLineResponse(
    val statementLineId: String,
    val journalEntryId: String,
    val amount: BigDecimal,
    val driftDays: Int,
)

data class AutoMatchResponse(
    val statementId: String,
    val matched: List<MatchedLineResponse>,
    val unmatchedLineIds: List<String>,
    val ambiguousLineIds: List<String>,
)

data class ReconciliationSummaryResponse(
    val bankAccountId: String,
    val glAccountId: String,
    val bankSideBalance: BigDecimal,
    val glSideBalance: BigDecimal,
    val variance: BigDecimal,
    val unreconciledLineCount: Long,
    val asOfDate: LocalDate,
)
