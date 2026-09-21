package com.aquinofroilan.tessera.domain.reporting.repository

import com.aquinofroilan.tessera.domain.reporting.model.KpiValue
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository
interface KpiValueRepository : JpaRepository<KpiValue, UUID> {
    fun findByKpiIdAndPeriodDateBetweenOrderByPeriodDateAsc(
        kpiId: UUID,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<KpiValue>

    fun findByKpiIdAndPeriodDate(
        kpiId: UUID,
        periodDate: LocalDate,
    ): KpiValue?
}
