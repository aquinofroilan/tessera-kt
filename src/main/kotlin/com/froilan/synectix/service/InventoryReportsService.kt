package com.froilan.synectix.service

import com.froilan.synectix.dto.MovementHistoryLineResponse
import com.froilan.synectix.dto.MovementHistoryResponse
import com.froilan.synectix.dto.StockOnHandLineResponse
import com.froilan.synectix.dto.StockOnHandReportResponse
import com.froilan.synectix.model.StockMovement
import com.froilan.synectix.model.StockMovementType
import com.froilan.synectix.repository.OnHandKey
import com.froilan.synectix.repository.StockMovementRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class InventoryReportsService(
    private val stockMovementRepository: StockMovementRepository,
) {
    fun stockOnHand(
        organizationId: String,
        productId: String?,
        warehouseId: String?,
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
        organizationId: String,
        productId: String?,
        warehouseId: String?,
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
                val pair = OnHandKey(m.productId, primaryAffectedWarehouse(m, warehouseId))
                val current = runningByPair[pair] ?: BigDecimal.ZERO
                val newBalance = current + signedQuantity(m, pair.warehouseId)
                runningByPair[pair] = newBalance
                MovementHistoryLineResponse(
                    id = m.id,
                    type = m.type,
                    productId = m.productId,
                    warehouseId = m.warehouseId,
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
        organizationId: String,
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
            if (m.type == StockMovementType.TRANSFER && m.transferToWarehouseId != null) {
                val dest = OnHandKey(m.productId, m.transferToWarehouseId)
                totals[dest] = (totals[dest] ?: BigDecimal.ZERO) + signedQuantity(m, m.transferToWarehouseId)
            }
        }
        return totals
    }

    private fun primaryAffectedWarehouse(
        movement: StockMovement,
        filterWarehouseId: String?,
    ): String =
        if (movement.type == StockMovementType.TRANSFER &&
            filterWarehouseId != null &&
            movement.transferToWarehouseId == filterWarehouseId
        ) {
            movement.transferToWarehouseId
        } else {
            movement.warehouseId
        }

    private fun signedQuantity(
        movement: StockMovement,
        forWarehouseId: String,
    ): BigDecimal {
        val q = movement.quantity
        return when (movement.type) {
            StockMovementType.RECEIPT,
            StockMovementType.OPENING_BALANCE,
            -> if (movement.warehouseId == forWarehouseId) q else BigDecimal.ZERO
            StockMovementType.ISSUE ->
                if (movement.warehouseId == forWarehouseId) q.negate() else BigDecimal.ZERO
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
