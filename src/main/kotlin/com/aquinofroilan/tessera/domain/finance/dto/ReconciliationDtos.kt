package com.aquinofroilan.tessera.domain.finance.dto

import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.time.LocalDate

data class ManualMatchRequest(
    @field:NotBlank(message = "Journal entry ID is required")
    val journalEntryId: java.util.UUID,
)

data class AutoMatchRequest(
    val maxDateDriftDays: Int = 5,
)

data class MatchedLineResponse(
    val statementLineId: java.util.UUID,
    val journalEntryId: java.util.UUID,
    val amount: BigDecimal,
    val driftDays: Int,
)

data class AutoMatchResponse(
    val statementId: java.util.UUID,
    val matched: List<MatchedLineResponse>,
    val unmatchedLineIds: List<java.util.UUID>,
    val ambiguousLineIds: List<java.util.UUID>,
)

data class ReconciliationSummaryResponse(
    val bankAccountId: java.util.UUID,
    val glAccountId: java.util.UUID,
    val bankSideBalance: BigDecimal,
    val glSideBalance: BigDecimal,
    val variance: BigDecimal,
    val unreconciledLineCount: Long,
    val asOfDate: LocalDate,
)
