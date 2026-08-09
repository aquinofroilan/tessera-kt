package com.aquinofroilan.tessera.dto

import java.math.BigDecimal
import java.time.LocalDate

data class CashFlowBucket(
    val periodEnd: LocalDate,
    val inflow: BigDecimal,
    val outflow: BigDecimal,
    val net: BigDecimal,
    val runningBalance: BigDecimal,
)

data class CashFlowForecastResponse(
    val asOfDate: LocalDate,
    val horizonEnd: LocalDate,
    val currency: String,
    val startingCash: BigDecimal,
    val totalInflow: BigDecimal,
    val totalOutflow: BigDecimal,
    val projectedEndingCash: BigDecimal,
    val overdueAr: BigDecimal,
    val overdueAp: BigDecimal,
    val buckets: List<CashFlowBucket>,
)
