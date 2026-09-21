package com.aquinofroilan.tessera.domain.reporting.dto

import com.aquinofroilan.tessera.domain.reporting.model.MetricType
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class KpiDefinitionRequest(
    val name: String,
    val description: String? = null,
    val metricType: MetricType,
    val targetValue: BigDecimal,
    val warningThreshold: BigDecimal? = null,
    val criticalThreshold: BigDecimal? = null,
    val higherIsBetter: Boolean = true,
)

data class KpiValueRequest(
    val periodDate: LocalDate,
    val actualValue: BigDecimal,
)

enum class KpiStatus {
    ON_TRACK,
    WARNING,
    CRITICAL,
}

data class KpiTrendDataPoint(
    val id: UUID,
    val periodDate: LocalDate,
    val actualValue: BigDecimal,
    val status: KpiStatus,
)

data class KpiTrendResponse(
    val kpiId: UUID,
    val name: String,
    val metricType: MetricType,
    val targetValue: BigDecimal,
    val warningThreshold: BigDecimal?,
    val criticalThreshold: BigDecimal?,
    val higherIsBetter: Boolean,
    val values: List<KpiTrendDataPoint>,
)
