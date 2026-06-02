package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.AssetDepreciationRun
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface AssetDepreciationRunRepository : JpaRepository<AssetDepreciationRun, String> {
    fun findByOrganizationIdOrderByPeriodYearDescPeriodMonthDesc(organizationId: String): List<AssetDepreciationRun>

    fun findByOrganizationIdAndPeriodYearAndPeriodMonth(
        organizationId: String,
        periodYear: Int,
        periodMonth: Int,
    ): Optional<AssetDepreciationRun>
}
