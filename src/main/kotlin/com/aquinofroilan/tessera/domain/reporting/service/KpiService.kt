package com.aquinofroilan.tessera.domain.reporting.service

import com.aquinofroilan.tessera.domain.reporting.dto.KpiDefinitionRequest
import com.aquinofroilan.tessera.domain.reporting.dto.KpiStatus
import com.aquinofroilan.tessera.domain.reporting.dto.KpiTrendDataPoint
import com.aquinofroilan.tessera.domain.reporting.dto.KpiTrendResponse
import com.aquinofroilan.tessera.domain.reporting.dto.KpiValueRequest
import com.aquinofroilan.tessera.domain.reporting.model.KpiDefinition
import com.aquinofroilan.tessera.domain.reporting.model.KpiValue
import com.aquinofroilan.tessera.domain.reporting.repository.KpiDefinitionRepository
import com.aquinofroilan.tessera.domain.reporting.repository.KpiValueRepository
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Service
class KpiService(
    private val kpiDefinitionRepository: KpiDefinitionRepository,
    private val kpiValueRepository: KpiValueRepository,
) {
    @Transactional(readOnly = true)
    fun listDefinitions(organizationId: UUID): List<KpiDefinition> = kpiDefinitionRepository.findByOrganizationId(organizationId)

    @Transactional
    fun createDefinition(
        organizationId: UUID,
        request: KpiDefinitionRequest,
    ): KpiDefinition {
        val definition =
            KpiDefinition(
                organizationId = organizationId,
                name = request.name,
                description = request.description,
                metricType = request.metricType,
                targetValue = request.targetValue,
                warningThreshold = request.warningThreshold,
                criticalThreshold = request.criticalThreshold,
                higherIsBetter = request.higherIsBetter,
            )
        return kpiDefinitionRepository.save(definition)
    }

    @Transactional
    fun recordValue(
        organizationId: UUID,
        kpiId: UUID,
        request: KpiValueRequest,
    ): KpiValue {
        val kpi =
            kpiDefinitionRepository.findByIdAndOrganizationId(kpiId, organizationId)
                ?: throw ResourceNotFoundException("KPI definition not found")

        val existing = kpiValueRepository.findByKpiIdAndPeriodDate(kpi.id, request.periodDate)
        if (existing != null) {
            existing.actualValue = request.actualValue
            return kpiValueRepository.save(existing)
        }

        val newValue =
            KpiValue(
                kpiId = kpi.id,
                periodDate = request.periodDate,
                actualValue = request.actualValue,
            )
        return kpiValueRepository.save(newValue)
    }

    @Transactional(readOnly = true)
    fun getKpiTrend(
        organizationId: UUID,
        kpiId: UUID,
        startDate: LocalDate,
        endDate: LocalDate,
    ): KpiTrendResponse {
        val kpi =
            kpiDefinitionRepository.findByIdAndOrganizationId(kpiId, organizationId)
                ?: throw ResourceNotFoundException("KPI definition not found")

        val values = kpiValueRepository.findByKpiIdAndPeriodDateBetweenOrderByPeriodDateAsc(kpi.id, startDate, endDate)

        val dataPoints =
            values.map { value ->
                KpiTrendDataPoint(
                    id = value.id,
                    periodDate = value.periodDate,
                    actualValue = value.actualValue,
                    status = calculateStatus(kpi, value.actualValue),
                )
            }

        return KpiTrendResponse(
            kpiId = kpi.id,
            name = kpi.name,
            metricType = kpi.metricType,
            targetValue = kpi.targetValue,
            warningThreshold = kpi.warningThreshold,
            criticalThreshold = kpi.criticalThreshold,
            higherIsBetter = kpi.higherIsBetter,
            values = dataPoints,
        )
    }

    private fun calculateStatus(
        kpi: KpiDefinition,
        actualValue: BigDecimal,
    ): KpiStatus {
        if (kpi.higherIsBetter) {
            if (kpi.criticalThreshold != null && actualValue < kpi.criticalThreshold!!) {
                return KpiStatus.CRITICAL
            }
            if (kpi.warningThreshold != null && actualValue < kpi.warningThreshold!!) {
                return KpiStatus.WARNING
            }
        } else {
            if (kpi.criticalThreshold != null && actualValue > kpi.criticalThreshold!!) {
                return KpiStatus.CRITICAL
            }
            if (kpi.warningThreshold != null && actualValue > kpi.warningThreshold!!) {
                return KpiStatus.WARNING
            }
        }
        return KpiStatus.ON_TRACK
    }
}
