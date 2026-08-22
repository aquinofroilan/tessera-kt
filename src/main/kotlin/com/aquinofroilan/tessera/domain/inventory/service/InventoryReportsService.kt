package com.aquinofroilan.tessera.domain.inventory.service

import com.aquinofroilan.tessera.domain.inventory.dto.MovementHistoryLineResponse
import com.aquinofroilan.tessera.domain.inventory.dto.MovementHistoryResponse
import com.aquinofroilan.tessera.domain.inventory.dto.StockOnHandLineResponse
import com.aquinofroilan.tessera.domain.inventory.dto.StockOnHandReportResponse
import com.aquinofroilan.tessera.domain.inventory.model.StockMovement
import com.aquinofroilan.tessera.domain.inventory.model.StockMovementType
import com.aquinofroilan.tessera.domain.inventory.repository.OnHandKey
import com.aquinofroilan.tessera.domain.inventory.repository.StockMovementRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class InventoryReportsService(
    private val stockMovementRepository: StockMovementRepository,
) {
    fun stockOnHand(
        organizationId: java.util.UUID,
        productId: java.util.UUID?,
        warehouseId: java.util.UUID?,
        asOfDate: LocalDateTime?,
    ): StockOnHandReportResponse {
        val totals: Map<OnHandKey, BigDecimal> =
            if (asOfDate == null) {
                stockMovementRepository.onHandByProductWarehouse(organizationId)
            } else {
                replayOnHand(organizationId, asOfDate)
            }
        val lines =
            totals
                .filter { (key, qty) ->
                    qty.signum() != 0 &&
                        (productId == null || key.productId == productId) &&
                        (warehouseId == null || key.warehouseId == warehouseId)
                }.map { (key, qty) ->
                    StockOnHandLineResponse(
                        productId = key.productId,
                        warehouseId = key.warehouseId,
                        quantity = qty,
                    )
                }.sortedWith(compareBy({ it.productId }, { it.warehouseId }))
        return StockOnHandReportResponse(asOfDate = asOfDate?.toString(), lines = lines)
    }

    fun movementHistory(
        organizationId: java.util.UUID,
        productId: java.util.UUID?,
        warehouseId: java.util.UUID?,
        from: LocalDateTime?,
        to: LocalDateTime?,
    ): MovementHistoryResponse {
        val movements =
            stockMovementRepository
                .listMovements(organizationId, productId, warehouseId, null, from, to)
                .sortedBy { it.occurredAt }
        val runningByPair = mutableMapOf<OnHandKey, BigDecimal>()
        val lines =
            movements.map { m ->
                val affectedWarehouseId = primaryAffectedWarehouse(m, warehouseId)
                val pair = OnHandKey(m.productId, affectedWarehouseId)
                val current = runningByPair[pair] ?: BigDecimal.ZERO
                val newBalance = current + signedQuantity(m, pair.warehouseId)
                runningByPair[pair] = newBalance
                MovementHistoryLineResponse(
                    id = m.id,
                    type = m.type,
                    productId = m.productId,
                    // Report the warehouse the running balance is keyed on so a
                    // caller filtering by a destination warehouse sees that
                    // warehouse on the line, not the TRANSFER's source side.
                    warehouseId = affectedWarehouseId,
                    transferToWarehouseId = m.transferToWarehouseId,
                    quantity = m.quantity,
                    unitCost = m.unitCost,
                    occurredAt = m.occurredAt.toString(),
                    runningBalance = newBalance,
                )
            }
        return MovementHistoryResponse(
            productId = productId,
            warehouseId = warehouseId,
            from = from?.toString(),
            to = to?.toString(),
            lines = lines,
        )
    }

    private fun replayOnHand(
        organizationId: java.util.UUID,
        asOfDate: LocalDateTime,
    ): Map<OnHandKey, BigDecimal> {
        val all =
            stockMovementRepository
                .listMovements(organizationId, null, null, null, null, asOfDate)
                .sortedBy { it.occurredAt }
        val totals = mutableMapOf<OnHandKey, BigDecimal>()
        for (m in all) {
            val primary = OnHandKey(m.productId, m.warehouseId)
            totals[primary] = (totals[primary] ?: BigDecimal.ZERO) + signedQuantity(m, m.warehouseId)
            val destId = m.transferToWarehouseId
            if (m.type == StockMovementType.TRANSFER && destId != null) {
                val dest = OnHandKey(m.productId, destId)
                totals[dest] = (totals[dest] ?: BigDecimal.ZERO) + signedQuantity(m, destId)
            }
        }
        return totals
    }

    private fun primaryAffectedWarehouse(
        movement: StockMovement,
        filterWarehouseId: java.util.UUID?,
    ): java.util.UUID {
        val destId = movement.transferToWarehouseId
        return if (movement.type == StockMovementType.TRANSFER &&
            filterWarehouseId != null &&
            destId == filterWarehouseId
        ) {
            destId
        } else {
            movement.warehouseId
        }
    }

    private fun signedQuantity(
        movement: StockMovement,
        forWarehouseId: java.util.UUID,
    ): BigDecimal {
        val q = movement.quantity
        return when (movement.type) {
            StockMovementType.RECEIPT,
            StockMovementType.OPENING_BALANCE,
            StockMovementType.WIP_RECEIPT,
            -> if (movement.warehouseId == forWarehouseId) q else BigDecimal.ZERO
            StockMovementType.ISSUE,
            StockMovementType.WIP_ISSUE,
            -> if (movement.warehouseId == forWarehouseId) q.negate() else BigDecimal.ZERO
            StockMovementType.ADJUSTMENT ->
                if (movement.warehouseId == forWarehouseId) q else BigDecimal.ZERO
            StockMovementType.TRANSFER ->
                when (forWarehouseId) {
                    movement.warehouseId -> q.negate()
                    movement.transferToWarehouseId -> q
                    else -> BigDecimal.ZERO
                }
        }
    }
}
