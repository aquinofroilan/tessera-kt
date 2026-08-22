package com.aquinofroilan.tessera.domain.assets.service

import com.aquinofroilan.tessera.domain.assets.model.AssetStatus
import com.aquinofroilan.tessera.domain.assets.model.DepreciationMethod
import com.aquinofroilan.tessera.domain.assets.model.FixedAsset
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth

/**
 * Pure calculator: given a [FixedAsset] and a target month, returns how
 * much depreciation should land on that month. No DB calls; no state.
 *
 * Convention:
 * - **Acquisition month onwards** counts. An asset bought any day in
 *   January gets the full January monthly amount; nothing before.
 * - **Straight-line only** for v1 (the SQL CHECK keeps the column open
 *   for DECLINING_BALANCE etc. without a schema change).
 * - **Salvage floor**: the cumulative depreciation never exceeds
 *   \`cost - salvage\`. The final month may be a partial amount that
 *   tops up to the floor exactly.
 * - Assets in **DISPOSED** or **FULLY_DEPRECIATED** status produce zero.
 */
@Component
class DepreciationCalculator {
    fun monthlyDepreciation(
        asset: FixedAsset,
        period: YearMonth,
    ): BigDecimal {
        if (asset.status != AssetStatus.ACTIVE) return BigDecimal.ZERO
        if (asset.depreciationMethod != DepreciationMethod.STRAIGHT_LINE) return BigDecimal.ZERO

        val depreciableBase = asset.acquisitionCost.subtract(asset.salvageValue)
        if (depreciableBase.signum() <= 0) return BigDecimal.ZERO

        val acquisitionPeriod = YearMonth.from(asset.acquisitionDate)
        if (period.isBefore(acquisitionPeriod)) return BigDecimal.ZERO

        val remaining = depreciableBase.subtract(asset.accumulatedDepreciation)
        if (remaining.signum() <= 0) return BigDecimal.ZERO

        val perMonth =
            depreciableBase.divide(
                BigDecimal.valueOf(asset.usefulLifeMonths.toLong()),
                ROUNDING_SCALE,
                RoundingMode.HALF_UP,
            )

        return perMonth.coerceAtMost(remaining).setScale(ROUNDING_SCALE, RoundingMode.HALF_UP)
    }

    companion object {
        private const val ROUNDING_SCALE = 4
    }
}
