package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateStockMovementRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.StockMovement
import com.aquinofroilan.tessera.model.StockMovementType
import com.aquinofroilan.tessera.model.Warehouse
import com.aquinofroilan.tessera.repository.StockMovementRepository
import com.aquinofroilan.tessera.repository.StockOnHandRepository
import com.aquinofroilan.tessera.repository.WarehouseRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class StockMovementService(
    private val stockMovementRepository: StockMovementRepository,
    private val warehouseRepository: WarehouseRepository,
    private val stockOnHandRepository: StockOnHandRepository,
    private val inventoryCostingService: InventoryCostingService,
    private val inventoryPostingService: InventoryPostingService,
) {
    @Transactional
    fun createMovement(
        request: CreateStockMovementRequest,
        organizationId: String,
        userId: String,
    ): StockMovement {
        val type = request.type ?: throw BusinessRuleException("Movement type is required")
        val quantity = request.quantity ?: throw BusinessRuleException("Quantity is required")
        validateQuantitySign(type, quantity)
        validateUnitCost(type, request.unitCost)
        validateTransferShape(type, request.warehouseId, request.transferToWarehouseId)
        val sourceWarehouse = loadActiveWarehouse(request.warehouseId, organizationId)
        val destWarehouse =
            if (type == StockMovementType.TRANSFER && request.transferToWarehouseId != null) {
                loadActiveWarehouse(request.transferToWarehouseId, organizationId)
            } else {
                null
            }
        applyToCounter(type, request, organizationId, quantity, sourceWarehouse, destWarehouse)

        val movement =
            StockMovement(
                organizationId = organizationId,
                type = type,
                productId = request.productId,
                warehouseId = request.warehouseId,
                transferToWarehouseId = request.transferToWarehouseId,
                quantity = quantity,
                unitCost = request.unitCost,
                reference = request.reference,
                notes = request.notes,
                occurredAt = request.occurredAt ?: LocalDateTime.now(),
                createdBy = userId,
            )
        val saved = stockMovementRepository.save(movement)
        val cost = inventoryCostingService.apply(saved)
        inventoryPostingService.postMovement(saved, cost)
        return saved
    }

    /**
     * Like [createMovement] but also returns the total cost that the costing
     * engine computed for the movement (sum of unit_cost × quantity, derived
     * from the FIFO layers / weighted-average snapshot for outbound moves, or
     * the supplied unit cost for inbound moves).
     *
     * Manufacturing execution uses this to capture WIP-issue cost so it can
     * roll into the WIP-receipt unit cost at completion.
     */
    @Transactional
    fun createMovementCapturingCost(
        request: CreateStockMovementRequest,
        organizationId: String,
        userId: String,
    ): Pair<StockMovement, BigDecimal> {
        val type = request.type ?: throw BusinessRuleException("Movement type is required")
        val quantity = request.quantity ?: throw BusinessRuleException("Quantity is required")
        validateQuantitySign(type, quantity)
        validateUnitCost(type, request.unitCost)
        validateTransferShape(type, request.warehouseId, request.transferToWarehouseId)
        val sourceWarehouse = loadActiveWarehouse(request.warehouseId, organizationId)
        val destWarehouse =
            if (type == StockMovementType.TRANSFER && request.transferToWarehouseId != null) {
                loadActiveWarehouse(request.transferToWarehouseId, organizationId)
            } else {
                null
            }
        applyToCounter(type, request, organizationId, quantity, sourceWarehouse, destWarehouse)
        val saved =
            stockMovementRepository.save(
                StockMovement(
                    organizationId = organizationId,
                    type = type,
                    productId = request.productId,
                    warehouseId = request.warehouseId,
                    transferToWarehouseId = request.transferToWarehouseId,
                    quantity = quantity,
                    unitCost = request.unitCost,
                    reference = request.reference,
                    notes = request.notes,
                    occurredAt = request.occurredAt ?: LocalDateTime.now(),
                    createdBy = userId,
                ),
            )
        val cost = inventoryCostingService.apply(saved)
        inventoryPostingService.postMovement(saved, cost)
        return saved to cost
    }

    @Transactional
    fun reverseMovement(
        movementId: String,
        organizationId: String,
        userId: String,
    ): StockMovement {
        val original =
            stockMovementRepository.findById(movementId).orElseThrow {
                ResourceNotFoundException("Stock movement not found")
            }
        if (original.organizationId != organizationId) {
            throw ResourceNotFoundException("Stock movement not found")
        }
        return reverse(original, organizationId, userId)
    }

    /** Reverses every not-yet-reversed movement carrying [reference]. Returns the compensating movements. */
    @Transactional
    fun reverseByReference(
        reference: String,
        organizationId: String,
        userId: String,
    ): List<StockMovement> =
        stockMovementRepository
            .findByOrganizationIdAndReference(organizationId, reference)
            .filter { !it.reversed && it.reversalOfMovementId == null }
            .map { reverse(it, organizationId, userId) }

    private fun reverse(
        original: StockMovement,
        organizationId: String,
        userId: String,
    ): StockMovement {
        if (original.reversed) {
            throw BusinessRuleException("Stock movement has already been reversed")
        }
        if (original.reversalOfMovementId != null) {
            throw BusinessRuleException("A reversal movement cannot itself be reversed")
        }
        if (original.type == StockMovementType.TRANSFER) {
            throw BusinessRuleException("Transfer movements cannot be reversed")
        }

        // Inverse on-hand effect: undo what the original did to the source warehouse.
        val inverseQuantity =
            when (original.type) {
                StockMovementType.RECEIPT,
                StockMovementType.OPENING_BALANCE,
                StockMovementType.WIP_RECEIPT,
                -> original.quantity.negate()
                StockMovementType.ISSUE,
                StockMovementType.WIP_ISSUE,
                -> original.quantity
                StockMovementType.ADJUSTMENT -> original.quantity.negate()
                StockMovementType.TRANSFER -> throw BusinessRuleException("unreachable")
            }
        // Re-adding stock needs a cost; reuse the original's when known, otherwise
        // let costing fall back to the current average.
        val unitCost = if (inverseQuantity.signum() > 0) original.unitCost else null

        val compensating =
            createMovement(
                CreateStockMovementRequest(
                    type = StockMovementType.ADJUSTMENT,
                    productId = original.productId,
                    warehouseId = original.warehouseId,
                    quantity = inverseQuantity,
                    unitCost = unitCost,
                    reference = "REVERSAL-${original.reference ?: original.id}",
                    notes = "Reversal of movement ${original.id}",
                ),
                organizationId,
                userId,
            )
        val marked = compensating.copy(reversalOfMovementId = original.id)
        val saved = stockMovementRepository.save(marked)
        stockMovementRepository.save(original.copy(reversed = true))
        return saved
    }

    fun listMovements(
        organizationId: String,
        productId: String? = null,
        warehouseId: String? = null,
        type: StockMovementType? = null,
        from: LocalDateTime? = null,
        to: LocalDateTime? = null,
    ): List<StockMovement> = stockMovementRepository.listMovements(organizationId, productId, warehouseId, type, from, to)

    fun onHand(
        organizationId: String,
        productId: String,
        warehouseId: String,
    ): BigDecimal = stockOnHandRepository.get(organizationId, productId, warehouseId)

    private fun validateQuantitySign(
        type: StockMovementType,
        quantity: BigDecimal,
    ) {
        when (type) {
            StockMovementType.RECEIPT,
            StockMovementType.ISSUE,
            StockMovementType.TRANSFER,
            StockMovementType.OPENING_BALANCE,
            StockMovementType.WIP_ISSUE,
            StockMovementType.WIP_RECEIPT,
            ->
                if (quantity.signum() <= 0) {
                    throw BusinessRuleException("Quantity must be positive for $type movements")
                }
            StockMovementType.ADJUSTMENT ->
                if (quantity.signum() == 0) {
                    throw BusinessRuleException("Adjustment quantity must be non-zero")
                }
        }
    }

    private fun validateUnitCost(
        type: StockMovementType,
        unitCost: BigDecimal?,
    ) {
        val inbound =
            type == StockMovementType.RECEIPT ||
                type == StockMovementType.OPENING_BALANCE ||
                type == StockMovementType.WIP_RECEIPT
        if (inbound) {
            if (unitCost == null || unitCost.signum() < 0) {
                throw BusinessRuleException("unitCost is required and must be zero or positive for $type movements")
            }
        }
    }

    private fun validateTransferShape(
        type: StockMovementType,
        warehouseId: String,
        transferToWarehouseId: String?,
    ) {
        if (type == StockMovementType.TRANSFER) {
            if (transferToWarehouseId.isNullOrBlank()) {
                throw BusinessRuleException("transferToWarehouseId is required for TRANSFER movements")
            }
            if (transferToWarehouseId == warehouseId) {
                throw BusinessRuleException("Transfer source and destination warehouses must differ")
            }
        } else if (transferToWarehouseId != null) {
            throw BusinessRuleException("transferToWarehouseId only applies to TRANSFER movements")
        }
    }

    private fun loadActiveWarehouse(
        warehouseId: String,
        organizationId: String,
    ): Warehouse {
        val warehouse =
            warehouseRepository.findById(warehouseId).orElseThrow {
                ResourceNotFoundException("Warehouse not found")
            }
        if (warehouse.organizationId != organizationId) {
            throw ResourceNotFoundException("Warehouse not found")
        }
        if (!warehouse.isActive) {
            throw BusinessRuleException("Warehouse '${warehouse.code}' is inactive")
        }
        return warehouse
    }

    private fun applyToCounter(
        type: StockMovementType,
        request: CreateStockMovementRequest,
        organizationId: String,
        quantity: BigDecimal,
        sourceWarehouse: Warehouse,
        destWarehouse: Warehouse?,
    ) {
        val sourceDelta = sourceDelta(type, quantity)
        if (sourceDelta.signum() != 0) {
            val ok =
                stockOnHandRepository.applyDelta(
                    organizationId,
                    request.productId,
                    sourceWarehouse.id,
                    sourceDelta,
                    allowNegative = sourceWarehouse.allowNegativeStock,
                )
            if (!ok) {
                val current = stockOnHandRepository.get(organizationId, request.productId, sourceWarehouse.id)
                throw BusinessRuleException(
                    "Movement would drive on-hand below zero in warehouse '${sourceWarehouse.code}' " +
                        "(current $current, requested $quantity); enable allowNegativeStock to permit",
                )
            }
        }

        if (type == StockMovementType.TRANSFER && destWarehouse != null) {
            // Crediting the destination can never violate the policy, so allowNegative=true.
            stockOnHandRepository.applyDelta(
                organizationId,
                request.productId,
                destWarehouse.id,
                quantity,
                allowNegative = true,
            )
        }
    }

    private fun sourceDelta(
        type: StockMovementType,
        quantity: BigDecimal,
    ): BigDecimal =
        when (type) {
            StockMovementType.RECEIPT,
            StockMovementType.OPENING_BALANCE,
            StockMovementType.WIP_RECEIPT,
            -> quantity
            StockMovementType.ISSUE,
            StockMovementType.TRANSFER,
            StockMovementType.WIP_ISSUE,
            -> quantity.negate()
            StockMovementType.ADJUSTMENT -> quantity
        }
}
