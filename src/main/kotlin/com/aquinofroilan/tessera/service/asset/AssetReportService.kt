package com.aquinofroilan.tessera.service.asset

import com.aquinofroilan.tessera.dto.AssetRegisterResponse
import com.aquinofroilan.tessera.dto.AssetRegisterRow
import com.aquinofroilan.tessera.dto.DepreciationScheduleResponse
import com.aquinofroilan.tessera.dto.DepreciationScheduleRow
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.AssetStatus
import com.aquinofroilan.tessera.model.FixedAsset
import com.aquinofroilan.tessera.repository.AssetCategoryRepository
import com.aquinofroilan.tessera.repository.FixedAssetRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.UUID

/**
 * Read-only reports over the fixed-asset state:
 *
 * - [assetRegister] is a current-state snapshot with category names joined
 *   and totals rolled up. Optional [status] and [categoryId] filters.
 * - [depreciationSchedule] projects forward [months] periods, simulating
 *   the monthly depreciation that *would* happen if every draft run was
 *   posted on schedule. Pure forward-looking math from the calculator —
 *   does not query any posted journal entries.
 */
@Service
class AssetReportService(
    private val fixedAssetRepository: FixedAssetRepository,
    private val assetCategoryRepository: AssetCategoryRepository,
    private val calculator: DepreciationCalculator,
) {
    fun assetRegister(
        organizationId: UUID,
        status: AssetStatus? = null,
        categoryId: UUID? = null,
    ): AssetRegisterResponse {
        val assets =
            when {
                status != null -> fixedAssetRepository.findByOrganizationIdAndStatus(organizationId, status)
                categoryId != null -> fixedAssetRepository.findByOrganizationIdAndCategoryId(organizationId, categoryId)
                else -> fixedAssetRepository.findByOrganizationId(organizationId)
            }

        val categoriesById =
            assetCategoryRepository
                .findByOrganizationId(organizationId)
                .associateBy { it.id }

        val rows =
            assets
                .sortedBy { it.assetNumber }
                .map { asset ->
                    val category = asset.categoryId?.let { categoriesById[it] }
                    AssetRegisterRow(
                        id = asset.id.toString(),
                        assetNumber = asset.assetNumber,
                        name = asset.name,
                        categoryCode = category?.code,
                        categoryName = category?.name,
                        acquisitionDate = asset.acquisitionDate.toString(),
                        acquisitionCost = asset.acquisitionCost,
                        salvageValue = asset.salvageValue,
                        accumulatedDepreciation = asset.accumulatedDepreciation,
                        netBookValue = asset.acquisitionCost.subtract(asset.accumulatedDepreciation),
                        usefulLifeMonths = asset.usefulLifeMonths,
                        depreciationMethod = asset.depreciationMethod,
                        status = asset.status,
                        location = asset.location,
                    )
                }

        val totalCost = rows.fold(BigDecimal.ZERO) { acc, r -> acc.add(r.acquisitionCost) }
        val totalAccum = rows.fold(BigDecimal.ZERO) { acc, r -> acc.add(r.accumulatedDepreciation) }
        val totalNbv = rows.fold(BigDecimal.ZERO) { acc, r -> acc.add(r.netBookValue) }

        return AssetRegisterResponse(
            rows = rows,
            totalAcquisitionCost = totalCost,
            totalAccumulatedDepreciation = totalAccum,
            totalNetBookValue = totalNbv,
        )
    }

    fun depreciationSchedule(
        organizationId: UUID,
        assetId: UUID? = null,
        months: Int = DEFAULT_SCHEDULE_MONTHS,
    ): DepreciationScheduleResponse {
        require(months in 1..MAX_SCHEDULE_MONTHS) { "months must be between 1 and $MAX_SCHEDULE_MONTHS" }

        val assets: List<FixedAsset> =
            if (assetId != null) {
                val asset =
                    fixedAssetRepository.findById(assetId).orElseThrow {
                        ResourceNotFoundException("Fixed asset $assetId not found")
                    }
                if (asset.organizationId != organizationId) {
                    throw ResourceNotFoundException("Fixed asset $assetId not found")
                }
                listOf(asset)
            } else {
                fixedAssetRepository.findByOrganizationIdAndStatus(organizationId, AssetStatus.ACTIVE)
            }

        val start = YearMonth.from(LocalDate.now(ZoneOffset.UTC))
        val rows =
            assets.flatMap { asset ->
                var cumulative = asset.accumulatedDepreciation
                (0 until months).mapNotNull { offset ->
                    val period = start.plusMonths(offset.toLong())
                    val simulated =
                        asset.copy(accumulatedDepreciation = cumulative)
                    val amount = calculator.monthlyDepreciation(simulated, period)
                    if (amount.signum() <= 0) {
                        null
                    } else {
                        cumulative = cumulative.add(amount)
                        DepreciationScheduleRow(
                            assetId = asset.id.toString(),
                            assetNumber = asset.assetNumber,
                            periodYear = period.year,
                            periodMonth = period.monthValue,
                            depreciationAmount = amount,
                            cumulativeDepreciation = cumulative,
                            netBookValue = asset.acquisitionCost.subtract(cumulative),
                        )
                    }
                }
            }

        return DepreciationScheduleResponse(
            rows = rows,
            months = months,
            assetCount = assets.size,
        )
    }

    companion object {
        private const val DEFAULT_SCHEDULE_MONTHS = 12
        private const val MAX_SCHEDULE_MONTHS = 240
    }
}
