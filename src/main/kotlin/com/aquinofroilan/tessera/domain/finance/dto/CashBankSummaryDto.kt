package com.aquinofroilan.tessera.domain.finance.dto

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class CashBankSummaryResponse(
    val accountId: UUID,
    val accountCode: String,
    val accountName: String,
    val type: String,
    val currentBalance: BigDecimal,
    val recentActivity: List<CashBankActivityLine>,
)

data class CashBankActivityLine(
    val journalEntryId: UUID,
    val entryDate: LocalDateTime,
    val description: String,
    val debit: BigDecimal,
    val credit: BigDecimal,
    val balanceAfter: BigDecimal? = null,
)
