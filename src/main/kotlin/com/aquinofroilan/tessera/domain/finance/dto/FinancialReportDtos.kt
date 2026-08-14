package com.aquinofroilan.tessera.domain.finance.dto

import java.math.BigDecimal

data class ReportAccountLine(
    val accountId: java.util.UUID,
    val accountCode: String,
    val accountName: String,
    val amount: BigDecimal,
    val comparativeAmount: BigDecimal? = null,
    val isSynthetic: Boolean = false,
)

object SyntheticAccountIds {
    const val CURRENT_PERIOD_EARNINGS = "__current_period_earnings__"
    val CURRENT_PERIOD_EARNINGS_ID = java.util.UUID.nameUUIDFromBytes("__current_period_earnings__".toByteArray())
}

data class ComparativePeriodMeta(
    val startDate: String,
    val endDate: String,
)

data class IncomeStatementResponse(
    val startDate: String,
    val endDate: String,
    val comparativePeriod: ComparativePeriodMeta? = null,
    val revenue: List<ReportAccountLine>,
    val totalRevenue: BigDecimal,
    val comparativeTotalRevenue: BigDecimal? = null,
    val expenses: List<ReportAccountLine>,
    val totalExpenses: BigDecimal,
    val comparativeTotalExpenses: BigDecimal? = null,
    val netIncome: BigDecimal,
    val comparativeNetIncome: BigDecimal? = null,
)

data class BalanceSheetResponse(
    val asOfDate: String,
    val comparativeAsOfDate: String? = null,
    val assets: List<ReportAccountLine>,
    val totalAssets: BigDecimal,
    val comparativeTotalAssets: BigDecimal? = null,
    val liabilities: List<ReportAccountLine>,
    val totalLiabilities: BigDecimal,
    val comparativeTotalLiabilities: BigDecimal? = null,
    val equity: List<ReportAccountLine>,
    val totalEquity: BigDecimal,
    val comparativeTotalEquity: BigDecimal? = null,
    val currentEarnings: BigDecimal,
    val comparativeCurrentEarnings: BigDecimal? = null,
    val totalLiabilitiesAndEquity: BigDecimal,
    val comparativeTotalLiabilitiesAndEquity: BigDecimal? = null,
    val isBalanced: Boolean,
    val outOfBalanceAmount: BigDecimal,
)

data class ComparativeTrialBalanceResponse(
    val current: TrialBalanceResponse,
    val comparative: TrialBalanceResponse? = null,
)
