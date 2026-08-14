package com.aquinofroilan.tessera.domain.inventory.service

import com.aquinofroilan.tessera.domain.inventory.model.InventoryCostLayer
import com.aquinofroilan.tessera.domain.inventory.model.InventoryWaSnapshot
import com.aquinofroilan.tessera.domain.inventory.model.StockMovement
import com.aquinofroilan.tessera.domain.inventory.model.StockMovementType
import com.aquinofroilan.tessera.domain.inventory.repository.InventoryCostLayerRepository
import com.aquinofroilan.tessera.domain.inventory.repository.InventoryWaSnapshotRepository
import com.aquinofroilan.tessera.domain.organization.model.InventoryCostingMethod
import com.aquinofroilan.tessera.domain.organization.repository.OrganizationRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class InventoryCostingService(
    private val layerRepository: InventoryCostLayerRepository,
    private val waSnapshotRepository: InventoryWaSnapshotRepository,
    private val organizationRepository: OrganizationRepository,
) {
    /**
     * Applies the movement to the cost layers / WA snapshot and returns the
     * absolute monetary cost of the movement, for downstream GL posting:
     * - inbound (RECEIPT/OPENING_BALANCE/positive ADJUSTMENT): quantity x unit cost added
     * - outbound (ISSUE/negative ADJUSTMENT/TRANSFER source): cost consumed
     * The value is always non-negative; the caller derives debit/credit direction.
     */
    @Transactional
    fun apply(movement: StockMovement): BigDecimal =
        when (costingMethodFor(movement.organizationId)) {
            InventoryCostingMethod.FIFO -> applyFifo(movement)
            InventoryCostingMethod.WEIGHTED_AVERAGE -> applyWeightedAverage(movement)
        }

    fun valuationCost(
        organizationId: java.util.UUID,
        productId: java.util.UUID,
        warehouseId: java.util.UUID,
    ): BigDecimal =
        when (costingMethodFor(organizationId)) {
            InventoryCostingMethod.FIFO ->
                layerRepository
                    .findByOrganizationIdAndProductIdAndWarehouseIdOrderByOccurredAtAsc(
                        organizationId,
                        productId,
                        warehouseId,
                    ).fold(BigDecimal.ZERO) { acc, layer -> acc + layer.remainingQuantity.multiply(layer.unitCost) }
            InventoryCostingMethod.WEIGHTED_AVERAGE ->
                waSnapshotRepository
                    .findByOrganizationIdAndProductIdAndWarehouseId(organizationId, productId, warehouseId)
                    .map { it.totalCost }
                    .orElse(BigDecimal.ZERO)
        }

    fun costingMethodFor(organizationId: java.util.UUID): InventoryCostingMethod =
        organizationRepository
            .findById(organizationId)
            .orElseThrow { ResourceNotFoundException("Organization not found") }
            .inventoryCostingMethod

    private fun applyFifo(movement: StockMovement): BigDecimal {
        val q = movement.quantity
        return when (movement.type) {
            StockMovementType.RECEIPT,
            StockMovementType.OPENING_BALANCE,
            StockMovementType.WIP_RECEIPT,
            -> addLayer(movement, q, movement.unitCost ?: BigDecimal.ZERO)
            StockMovementType.ISSUE,
            StockMovementType.WIP_ISSUE,
            -> consumeFifo(movement, movement.warehouseId, q)
            StockMovementType.ADJUSTMENT ->
                if (q.signum() > 0) {
                    addLayer(movement, q, movement.unitCost ?: currentAverageOrZero(movement))
                } else {
                    consumeFifo(movement, movement.warehouseId, q.abs())
                }
            StockMovementType.TRANSFER -> {
                val consumed = consumeFifo(movement, movement.warehouseId, q)
                val destId = movement.transferToWarehouseId
                if (destId != null) {
                    val avgCost =
                        if (consumed.signum() > 0 && q.signum() > 0) {
                            consumed.divide(q, 6, RoundingMode.HALF_UP)
                        } else {
                            BigDecimal.ZERO
                        }
                    layerRepository.save(
                        InventoryCostLayer(
                            organizationId = movement.organizationId,
                            productId = movement.productId,
                            warehouseId = destId,
                            originalQuantity = q,
                            remainingQuantity = q,
                            unitCost = avgCost,
                            sourceMovementId = movement.id,
                            occurredAt = movement.occurredAt,
                        ),
                    )
                }
                consumed
            }
        }
    }

    private fun addLayer(
        movement: StockMovement,
        quantity: BigDecimal,
        unitCost: BigDecimal,
    ): BigDecimal {
        layerRepository.save(
            InventoryCostLayer(
                organizationId = movement.organizationId,
                productId = movement.productId,
                warehouseId = movement.warehouseId,
                originalQuantity = quantity,
                remainingQuantity = quantity,
                unitCost = unitCost,
                sourceMovementId = movement.id,
                occurredAt = movement.occurredAt,
            ),
        )
        return quantity.multiply(unitCost)
    }

    private fun consumeFifo(
        movement: StockMovement,
        warehouseId: java.util.UUID,
        quantity: BigDecimal,
    ): BigDecimal {
        var remaining = quantity
        var totalCost = BigDecimal.ZERO
        val layers =
            layerRepository.findByOrganizationIdAndProductIdAndWarehouseIdOrderByOccurredAtAsc(
                movement.organizationId,
                movement.productId,
                warehouseId,
            )
        for (layer in layers) {
            if (remaining.signum() == 0) break
            if (layer.remainingQuantity.signum() == 0) continue
            val take = layer.remainingQuantity.min(remaining)
            totalCost += take.multiply(layer.unitCost)
            remaining -= take
            layer.remainingQuantity = layer.remainingQuantity - take
            layerRepository.save(layer)
        }
        if (remaining.signum() > 0) {
            // Negative-stock policy is enforced upstream in StockMovementService.
            // Reaching here means allowNegativeStock=true; emit a zero-cost layer so qty stays
            // accurate but value isn't inflated.
            return totalCost
        }
        return totalCost
    }

    private fun currentAverageOrZero(movement: StockMovement): BigDecimal {
        val layers =
            layerRepository.findByOrganizationIdAndProductIdAndWarehouseIdOrderByOccurredAtAsc(
                movement.organizationId,
                movement.productId,
                movement.warehouseId,
            )
        val totalQty = layers.fold(BigDecimal.ZERO) { acc, l -> acc + l.remainingQuantity }
        if (totalQty.signum() <= 0) return BigDecimal.ZERO
        val totalCost = layers.fold(BigDecimal.ZERO) { acc, l -> acc + l.remainingQuantity.multiply(l.unitCost) }
        return totalCost.divide(totalQty, 6, RoundingMode.HALF_UP)
    }

    private fun applyWeightedAverage(movement: StockMovement): BigDecimal {
        val q = movement.quantity
        return when (movement.type) {
            StockMovementType.RECEIPT,
            StockMovementType.OPENING_BALANCE,
            StockMovementType.WIP_RECEIPT,
            -> addToWa(movement, movement.warehouseId, q, movement.unitCost ?: BigDecimal.ZERO)
            StockMovementType.ISSUE,
            StockMovementType.WIP_ISSUE,
            -> consumeWa(movement, movement.warehouseId, q)
            StockMovementType.ADJUSTMENT ->
                if (q.signum() > 0) {
                    addToWa(movement, movement.warehouseId, q, movement.unitCost ?: avgWa(movement, movement.warehouseId))
                } else {
                    consumeWa(movement, movement.warehouseId, q.abs())
                }
            StockMovementType.TRANSFER -> {
                val consumedCost = consumeWa(movement, movement.warehouseId, q)
                val destId = movement.transferToWarehouseId
                if (destId != null) {
                    val unitCost =
                        if (q.signum() > 0) consumedCost.divide(q, 6, RoundingMode.HALF_UP) else BigDecimal.ZERO
                    addToWa(movement, destId, q, unitCost)
                }
                consumedCost
            }
        }
    }

    private fun addToWa(
        movement: StockMovement,
        warehouseId: java.util.UUID,
        quantity: BigDecimal,
        unitCost: BigDecimal,
    ): BigDecimal {
        val existing =
            waSnapshotRepository.findByOrganizationIdAndProductIdAndWarehouseId(
                movement.organizationId,
                movement.productId,
                warehouseId,
            )
        val newQty = existing.map { it.quantity }.orElse(BigDecimal.ZERO) + quantity
        val newTotal = existing.map { it.totalCost }.orElse(BigDecimal.ZERO) + quantity.multiply(unitCost)
        val snapshot =
            existing
                .map {
                    it.apply {
                        this.quantity = newQty
                        this.totalCost = newTotal
                    }
                }.orElseGet {
                    InventoryWaSnapshot(
                        organizationId = movement.organizationId,
                        productId = movement.productId,
                        warehouseId = warehouseId,
                        quantity = newQty,
                        totalCost = newTotal,
                    )
                }
        waSnapshotRepository.save(snapshot)
        return quantity.multiply(unitCost)
    }

    private fun consumeWa(
        movement: StockMovement,
        warehouseId: java.util.UUID,
        quantity: BigDecimal,
    ): BigDecimal {
        val existing =
            waSnapshotRepository
                .findByOrganizationIdAndProductIdAndWarehouseId(
                    movement.organizationId,
                    movement.productId,
                    warehouseId,
                ).orElse(null) ?: return BigDecimal.ZERO
        val avgCost =
            if (existing.quantity.signum() > 0) {
                existing.totalCost.divide(existing.quantity, 6, RoundingMode.HALF_UP)
            } else {
                BigDecimal.ZERO
            }
        val consumedCost = quantity.multiply(avgCost)
        val newQty = existing.quantity - quantity
        val newTotal = (existing.totalCost - consumedCost).max(BigDecimal.ZERO)
        existing.quantity = newQty
        existing.totalCost = newTotal
        waSnapshotRepository.save(existing)
        return consumedCost
    }

    private fun avgWa(
        movement: StockMovement,
        warehouseId: java.util.UUID,
    ): BigDecimal {
        val existing =
            waSnapshotRepository
                .findByOrganizationIdAndProductIdAndWarehouseId(
                    movement.organizationId,
                    movement.productId,
                    warehouseId,
                ).orElse(null) ?: return BigDecimal.ZERO
        if (existing.quantity.signum() <= 0) return BigDecimal.ZERO
        return existing.totalCost.divide(existing.quantity, 6, RoundingMode.HALF_UP)
    }

    @Suppress("unused")
    private fun unreachable(): Nothing = throw BusinessRuleException("unreachable")
}
