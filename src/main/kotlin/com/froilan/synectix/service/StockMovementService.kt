package com.froilan.synectix.service

import com.froilan.synectix.dto.CreateStockMovementRequest
import com.froilan.synectix.exception.BusinessRuleException
import com.froilan.synectix.exception.ResourceNotFoundException
import com.froilan.synectix.model.StockMovement
import com.froilan.synectix.model.StockMovementType
import com.froilan.synectix.model.Warehouse
import com.froilan.synectix.repository.StockMovementRepository
import com.froilan.synectix.repository.StockOnHandRepository
import com.froilan.synectix.repository.WarehouseRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class StockMovementService(
    private val stockMovementRepository: StockMovementRepository,
    private val warehouseRepository: WarehouseRepository,
    private val stockOnHandRepository: StockOnHandRepository,
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
        return stockMovementRepository.save(movement)
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
        val inbound = type == StockMovementType.RECEIPT || type == StockMovementType.OPENING_BALANCE
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
            -> quantity
            StockMovementType.ISSUE,
            StockMovementType.TRANSFER,
            -> quantity.negate()
            StockMovementType.ADJUSTMENT -> quantity
        }
}
