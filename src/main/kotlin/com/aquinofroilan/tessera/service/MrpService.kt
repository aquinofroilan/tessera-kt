package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.MrpRequirementLine
import com.aquinofroilan.tessera.dto.MrpRunResponse
import com.aquinofroilan.tessera.model.BomStatus
import com.aquinofroilan.tessera.model.MpsEntry
import com.aquinofroilan.tessera.model.MpsStatus
import com.aquinofroilan.tessera.repository.MpsEntryRepository
import com.aquinofroilan.tessera.repository.StockOnHandRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

/**
 * Single-level MRP run.
 *
 * Reads PLANNED + FIRM MPS entries up to `horizonEnd`, walks each parent's
 * default ACTIVE BOM, sums gross component requirements (scrap-adjusted),
 * subtracts current on-hand (summed across warehouses), and returns the
 * net requirement per component plus the earliest required-by date.
 *
 * Single-level only: sub-assemblies appear in the output as themselves,
 * not as their components. Callers cascade by re-running with the
 * sub-assemblies treated as MPS demand in a follow-up call.
 */
@Service
class MrpService(
    private val mpsRepository: MpsEntryRepository,
    private val bomService: BillOfMaterialsService,
    private val stockOnHandRepository: StockOnHandRepository,
) {
    fun run(
        organizationId: java.util.UUID,
        horizonEnd: LocalDate?,
    ): MrpRunResponse {
        val entries = loadEntries(organizationId, horizonEnd)
        if (entries.isEmpty()) {
            return MrpRunResponse(
                horizonEnd = horizonEnd,
                mpsEntriesConsidered = 0,
                requirements = emptyList(),
                unresolved = emptyList(),
            )
        }

        data class Accum(
            var gross: BigDecimal,
            var earliest: LocalDate,
            val sku: String,
            val name: String,
        )
        val acc = mutableMapOf<java.util.UUID, Accum>()
        val unresolved = mutableListOf<String>()
        for (mps in entries) {
            val bom =
                bomService
                    .listBoms(organizationId, BomStatus.ACTIVE, mps.productId)
                    .firstOrNull { it.isDefault }
            if (bom == null) {
                unresolved.add("No default ACTIVE BOM for product ${mps.productSku} (id=${mps.productId})")
                continue
            }
            for (line in bom.lines) {
                val factor = BigDecimal.ONE.add(line.scrapPct.divide(BigDecimal(100), 6, RoundingMode.HALF_UP))
                val grossForLine = line.quantity.multiply(mps.quantity).multiply(factor)
                val existing = acc[line.componentProductId]
                if (existing == null) {
                    acc[line.componentProductId] =
                        Accum(grossForLine, mps.requiredBy, line.componentSku, line.componentName)
                } else {
                    existing.gross = existing.gross.add(grossForLine)
                    if (mps.requiredBy.isBefore(existing.earliest)) {
                        existing.earliest = mps.requiredBy
                    }
                }
            }
        }

        val onHandByProduct =
            stockOnHandRepository
                .findByOrganizationId(organizationId)
                .groupBy { it.productId }
                .mapValues { (_, rows) -> rows.fold(BigDecimal.ZERO) { sum, soh -> sum.add(soh.quantity) } }

        val requirements =
            acc
                .map { (productId, a) ->
                    val onHand = onHandByProduct[productId] ?: BigDecimal.ZERO
                    val net = a.gross.subtract(onHand).max(BigDecimal.ZERO)
                    MrpRequirementLine(
                        productId = productId,
                        productSku = a.sku,
                        productName = a.name,
                        grossRequirement = a.gross.setScale(4, RoundingMode.HALF_UP),
                        onHand = onHand.setScale(4, RoundingMode.HALF_UP),
                        netRequirement = net.setScale(4, RoundingMode.HALF_UP),
                        earliestRequiredBy = a.earliest,
                    )
                }.sortedBy { it.earliestRequiredBy }

        return MrpRunResponse(
            horizonEnd = horizonEnd,
            mpsEntriesConsidered = entries.size,
            requirements = requirements,
            unresolved = unresolved,
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
