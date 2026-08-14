package com.aquinofroilan.tessera.domain.assets.repository

import com.aquinofroilan.tessera.domain.assets.model.AssetDepreciationRun
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
