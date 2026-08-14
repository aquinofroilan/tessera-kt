package com.aquinofroilan.tessera.service.asset

import com.aquinofroilan.tessera.model.AssetStatus
import com.aquinofroilan.tessera.model.DepreciationMethod
import com.aquinofroilan.tessera.model.FixedAsset
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class DepreciationCalculatorTest {
    private val calculator = DepreciationCalculator()

    @Test
    fun `straight line monthly amount is (cost minus salvage) over useful life`() {
        val asset =
            asset(
                acquisitionCost = BigDecimal("12000"),
                salvageValue = BigDecimal("1200"),
                usefulLifeMonths = 36,
                acquisitionDate = LocalDate.of(2026, 1, 1),
            )

        // (12000 - 1200) / 36 = 300.0000
        val amount = calculator.monthlyDepreciation(asset, YearMonth.of(2026, 6))

        assertThat(amount).isEqualByComparingTo("300.0000")
    }

    @Test
    fun `zero before acquisition month even when the asset is otherwise eligible`() {
        val asset =
            asset(
                acquisitionDate = LocalDate.of(2026, 5, 15),
                acquisitionCost = BigDecimal("3600"),
                salvageValue = BigDecimal.ZERO,
                usefulLifeMonths = 36,
            )

        assertThat(calculator.monthlyDepreciation(asset, YearMonth.of(2026, 4))).isEqualByComparingTo(BigDecimal.ZERO)
        // Acquisition month gets the full monthly amount per the documented
        // 'acquisition month onwards' convention.
        assertThat(calculator.monthlyDepreciation(asset, YearMonth.of(2026, 5))).isEqualByComparingTo("100.0000")
    }

    @Test
    fun `the final month is partial when the salvage floor would be exceeded`() {
        val asset =
            asset(
                acquisitionCost = BigDecimal("1000"),
                salvageValue = BigDecimal("100"),
                usefulLifeMonths = 9,
                acquisitionDate = LocalDate.of(2026, 1, 1),
                accumulatedDepreciation = BigDecimal("850"),
            )

        // depreciable base 900, already 850 used → remaining 50, perMonth 100 → cap at 50.
        val amount = calculator.monthlyDepreciation(asset, YearMonth.of(2026, 10))

        assertThat(amount).isEqualByComparingTo("50.0000")
    }

    @Test
    fun `DISPOSED or FULLY_DEPRECIATED assets contribute zero`() {
        val active = asset()
        val disposed = active.copy(status = AssetStatus.DISPOSED)
        val fullyDepreciated = active.copy(status = AssetStatus.FULLY_DEPRECIATED)

        assertThat(calculator.monthlyDepreciation(disposed, YearMonth.of(2026, 6))).isEqualByComparingTo(BigDecimal.ZERO)
        assertThat(calculator.monthlyDepreciation(fullyDepreciated, YearMonth.of(2026, 6))).isEqualByComparingTo(BigDecimal.ZERO)
    }

    @Test
    fun `a fully accumulated asset stops accruing further depreciation`() {
        val asset =
            asset(
                acquisitionCost = BigDecimal("1000"),
                salvageValue = BigDecimal("100"),
                usefulLifeMonths = 9,
                accumulatedDepreciation = BigDecimal("900"),
            )

        assertThat(calculator.monthlyDepreciation(asset, YearMonth.of(2026, 12)))
            .isEqualByComparingTo(BigDecimal.ZERO)
    }

    private fun asset(
        acquisitionCost: BigDecimal = BigDecimal("3600"),
        salvageValue: BigDecimal = BigDecimal.ZERO,
        usefulLifeMonths: Int = 36,
        acquisitionDate: LocalDate = LocalDate.of(2026, 1, 1),
        accumulatedDepreciation: BigDecimal = BigDecimal.ZERO,
    ): FixedAsset =
        FixedAsset(
            organizationId = java.util.UUID.randomUUID(),
            assetNumber = "FA-00001",
            name = "Test asset",
            acquisitionDate = acquisitionDate,
            acquisitionCost = acquisitionCost,
            salvageValue = salvageValue,
            usefulLifeMonths = usefulLifeMonths,
            depreciationMethod = DepreciationMethod.STRAIGHT_LINE,
            status = AssetStatus.ACTIVE,
            accumulatedDepreciation = accumulatedDepreciation,
        )
}
