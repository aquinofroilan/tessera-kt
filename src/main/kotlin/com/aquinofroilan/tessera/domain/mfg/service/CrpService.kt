package com.aquinofroilan.tessera.domain.mfg.service

import com.aquinofroilan.tessera.domain.mfg.model.MpsEntry
import com.aquinofroilan.tessera.domain.mfg.model.MpsStatus
import com.aquinofroilan.tessera.domain.mfg.model.RoutingStatus
import com.aquinofroilan.tessera.domain.mfg.repository.MpsEntryRepository
import com.aquinofroilan.tessera.domain.platform.dto.CrpLoadLine
import com.aquinofroilan.tessera.domain.platform.dto.CrpRunResponse
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Capacity Requirements Planning.
 *
 * For each MPS entry (PLANNED + FIRM) with a default ACTIVE routing,
 * sums setup + run*quantity minutes per work center and compares against
 * the work center's capacity over the horizon: capacity_minutes =
 * working_days * working_hours_per_day * 60 * (efficiency_pct / 100).
 *
 * "Working days" is approximated as calendar days from `today` to the
 * latest required-by within the considered MPS set (or `horizonEnd`).
 * A real shop calendar with holidays is out of scope for this slice.
 */
@Service
class CrpService(
    private val mpsRepository: MpsEntryRepository,
    private val routingService: RoutingService,
    private val workCenterService: WorkCenterService,
) {
    fun run(
        organizationId: java.util.UUID,
        horizonEnd: LocalDate?,
        capacityHoursPerWorkingDay: BigDecimal,
    ): CrpRunResponse {
        val entries = loadEntries(organizationId, horizonEnd)
        if (entries.isEmpty()) {
            return CrpRunResponse(
                horizonEnd = horizonEnd,
                mpsEntriesConsidered = 0,
                capacityHoursPerWorkingDay = capacityHoursPerWorkingDay,
                loads = emptyList(),
            )
        }

        data class Load(
            var requiredMinutes: BigDecimal,
            val code: String,
        )
        val load = mutableMapOf<java.util.UUID, Load>()
        for (mps in entries) {
            val routing =
                routingService
                    .listRoutings(organizationId, RoutingStatus.ACTIVE, mps.productId)
                    .firstOrNull { it.isDefault } ?: continue
            for (op in routing.operations) {
                val minutes = op.setupMinutes.add(op.runMinutesPerUnit.multiply(mps.quantity))
                val existing = load[op.workCenterId]
                if (existing == null) {
                    load[op.workCenterId] = Load(minutes, op.workCenterCode)
                } else {
                    existing.requiredMinutes = existing.requiredMinutes.add(minutes)
                }
            }
        }

        val latest = entries.maxOf { it.requiredBy }
        val effectiveEnd = horizonEnd ?: latest
        val workingDays = maxOf(1, ChronoUnit.DAYS.between(LocalDate.now(), effectiveEnd).toInt())

        val loads =
            load
                .map { (wcId, l) ->
                    val wc = workCenterService.getWorkCenter(wcId, organizationId)
                    val efficiency = wc.efficiencyPct.divide(BigDecimal(100), 6, RoundingMode.HALF_UP)
                    val capacityMinutes =
                        BigDecimal(workingDays)
                            .multiply(capacityHoursPerWorkingDay)
                            .multiply(BigDecimal(60))
                            .multiply(efficiency)
                            .setScale(4, RoundingMode.HALF_UP)
                    val utilisationPct =
                        if (capacityMinutes.signum() > 0) {
                            l.requiredMinutes
                                .divide(capacityMinutes, 6, RoundingMode.HALF_UP)
                                .multiply(BigDecimal(100))
                                .setScale(2, RoundingMode.HALF_UP)
                        } else {
                            BigDecimal.ZERO
                        }
                    CrpLoadLine(
                        workCenterId = wcId,
                        workCenterCode = l.code,
                        requiredMinutes = l.requiredMinutes.setScale(4, RoundingMode.HALF_UP),
                        capacityMinutes = capacityMinutes,
                        utilisationPct = utilisationPct,
                        overloaded = l.requiredMinutes > capacityMinutes,
                    )
                }.sortedByDescending { it.utilisationPct }

        return CrpRunResponse(
            horizonEnd = effectiveEnd,
            mpsEntriesConsidered = entries.size,
            capacityHoursPerWorkingDay = capacityHoursPerWorkingDay,
            loads = loads,
        )
    }

    private fun loadEntries(
        organizationId: java.util.UUID,
        horizonEnd: LocalDate?,
    ): List<MpsEntry> {
        val statuses = listOf(MpsStatus.PLANNED, MpsStatus.FIRM)
        return if (horizonEnd != null) {
            mpsRepository.findByOrganizationIdAndRequiredByLessThanEqualAndStatusInOrderByRequiredByAsc(
                organizationId,
                horizonEnd,
                statuses,
            )
        } else {
            mpsRepository
                .findByOrganizationIdOrderByRequiredByAsc(organizationId)
                .filter { it.status in statuses }
        }
    }
}
