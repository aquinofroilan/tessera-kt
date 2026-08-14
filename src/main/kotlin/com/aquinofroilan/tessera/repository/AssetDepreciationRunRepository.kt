package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.AssetDepreciationRun
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface AssetDepreciationRunRepository : JpaRepository<AssetDepreciationRun, UUID> {
    fun findByOrganizationIdOrderByPeriodYearDescPeriodMonthDesc(organizationId: UUID): List<AssetDepreciationRun>

    fun findByOrganizationIdAndPeriodYearAndPeriodMonth(
        organizationId: UUID,
        periodYear: Int,
        periodMonth: Int,
    ): Optional<AssetDepreciationRun>
}
