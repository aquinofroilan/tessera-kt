package com.aquinofroilan.tessera.domain.inventory.service

import com.aquinofroilan.tessera.domain.inventory.dto.CreateStockMovementRequest
import com.aquinofroilan.tessera.domain.inventory.dto.LotOnHandResponse
import com.aquinofroilan.tessera.domain.inventory.model.Product
import com.aquinofroilan.tessera.domain.inventory.model.ProductSerial
import com.aquinofroilan.tessera.domain.inventory.model.SerialStatus
import com.aquinofroilan.tessera.domain.inventory.model.StockMovement
import com.aquinofroilan.tessera.domain.inventory.model.StockMovementType
import com.aquinofroilan.tessera.domain.inventory.model.Warehouse
import com.aquinofroilan.tessera.domain.inventory.repository.ProductRepository
import com.aquinofroilan.tessera.domain.inventory.repository.StockMovementRepository
import com.aquinofroilan.tessera.domain.inventory.repository.StockOnHandRepository
import com.aquinofroilan.tessera.domain.inventory.repository.WarehouseRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
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
    private val productRepository: ProductRepository,
    private val productSerialRepository: com.aquinofroilan.tessera.domain.inventory.repository.ProductSerialRepository,
    private val productLotRepository: com.aquinofroilan.tessera.domain.inventory.repository.ProductLotRepository,
) {
    @Transactional
    fun createMovement(
        request: CreateStockMovementRequest,
        organizationId: java.util.UUID,
        userId: java.util.UUID,
    ): StockMovement {
        val type = request.type ?: throw BusinessRuleException("Movement type is required")
        val quantity = request.quantity ?: throw BusinessRuleException("Quantity is required")
        validateQuantitySign(type, quantity)
        validateUnitCost(type, request.unitCost)
        validateTransferShape(type, request.warehouseId, request.transferToWarehouseId)

        val product =
            productRepository.findById(request.productId).orElseThrow {
                ResourceNotFoundException("Product not found")
            }
        if (product.organizationId != organizationId) {
            throw ResourceNotFoundException("Product not found")
        }

        val resolvedLot = validateLotTracking(product, request.lotNumber)
        validateAndProcessSerials(
            product,
            type,
            quantity,
            request.serialNumbers,
            request.warehouseId,
            request.transferToWarehouseId,
            request.sourceLocationId,
            request.destinationLocationId,
            resolvedLot,
            organizationId,
        )
        validateAndProcessExpiry(product, type, resolvedLot, request.expiryDate, organizationId)
        val sourceWarehouse = loadActiveWarehouse(request.warehouseId, organizationId)
        val destWarehouse =
            if (type == StockMovementType.TRANSFER && request.transferToWarehouseId != null) {
                loadActiveWarehouse(request.transferToWarehouseId, organizationId)
            } else {
                null
            }
        applyToCounter(
            type, request, resolvedLot, organizationId, quantity, sourceWarehouse, destWarehouse,
            request.sourceLocationId, request.destinationLocationId
        )

        val movement =
            StockMovement(
                organizationId = organizationId,
                type = type,
                productId = request.productId,
                warehouseId = request.warehouseId,
                transferToWarehouseId = request.transferToWarehouseId,
                sourceLocationId = request.sourceLocationId,
                destinationLocationId = request.destinationLocationId,
                lotNumber = resolvedLot,
                serialNumbers = request.serialNumbers,
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
        organizationId: java.util.UUID,
        userId: java.util.UUID,
    ): Pair<StockMovement, BigDecimal> {
        val type = request.type ?: throw BusinessRuleException("Movement type is required")
        val quantity = request.quantity ?: throw BusinessRuleException("Quantity is required")
        validateQuantitySign(type, quantity)
        validateUnitCost(type, request.unitCost)
        validateTransferShape(type, request.warehouseId, request.transferToWarehouseId)

        val product =
            productRepository.findById(request.productId).orElseThrow {
                ResourceNotFoundException("Product not found")
            }
        if (product.organizationId != organizationId) {
            throw ResourceNotFoundException("Product not found")
        }

        val resolvedLot = validateLotTracking(product, request.lotNumber)
        validateAndProcessSerials(
            product,
            type,
            quantity,
            request.serialNumbers,
            request.warehouseId,
            request.transferToWarehouseId,
            request.sourceLocationId,
            request.destinationLocationId,
            resolvedLot,
            organizationId,
        )
        validateAndProcessExpiry(product, type, resolvedLot, request.expiryDate, organizationId)
        val sourceWarehouse = loadActiveWarehouse(request.warehouseId, organizationId)
        val destWarehouse =
            if (type == StockMovementType.TRANSFER && request.transferToWarehouseId != null) {
                loadActiveWarehouse(request.transferToWarehouseId, organizationId)
            } else {
                null
            }
        applyToCounter(
            type, request, resolvedLot, organizationId, quantity, sourceWarehouse, destWarehouse,
            request.sourceLocationId, request.destinationLocationId
        )
        val saved =
            stockMovementRepository.save(
                StockMovement(
                    organizationId = organizationId,
                    type = type,
                    productId = request.productId,
                    warehouseId = request.warehouseId,
                    transferToWarehouseId = request.transferToWarehouseId,
                    sourceLocationId = request.sourceLocationId,
                    destinationLocationId = request.destinationLocationId,
                    lotNumber = resolvedLot,
                    serialNumbers = request.serialNumbers,
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
        movementId: java.util.UUID,
        organizationId: java.util.UUID,
        userId: java.util.UUID,
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
        organizationId: java.util.UUID,
        userId: java.util.UUID,
    ): List<StockMovement> =
        stockMovementRepository
            .findByOrganizationIdAndReference(organizationId, reference)
            .filter { !it.reversed && it.reversalOfMovementId == null }
            .map { reverse(it, organizationId, userId) }

    private fun reverse(
        original: StockMovement,
        organizationId: java.util.UUID,
        userId: java.util.UUID,
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
                    lotNumber = original.lotNumber,
                    quantity = inverseQuantity,
                    unitCost = unitCost,
                    reference = "REVERSAL-${original.reference ?: original.id}",
                    notes = "Reversal of movement ${original.id}",
                ),
                organizationId,
                userId,
            )
        compensating.reversalOfMovementId = original.id
        val saved = stockMovementRepository.save(compensating)
        original.reversed = true
        stockMovementRepository.save(original)
        return saved
    }

    fun listMovements(
        organizationId: java.util.UUID,
        productId: java.util.UUID? = null,
        warehouseId: java.util.UUID? = null,
        type: StockMovementType? = null,
        from: LocalDateTime? = null,
        to: LocalDateTime? = null,
        lotNumber: String? = null,
    ): List<StockMovement> = stockMovementRepository.listMovements(organizationId, productId, warehouseId, type, from, to, lotNumber)

    fun onHand(
        organizationId: java.util.UUID,
        productId: java.util.UUID,
        warehouseId: java.util.UUID,
        lotNumber: String? = null,
    ): BigDecimal = stockOnHandRepository.get(organizationId, productId, warehouseId, lotNumber)

    fun getLotBreakdown(
        organizationId: java.util.UUID,
        productId: java.util.UUID,
        warehouseId: java.util.UUID,
    ): List<LotOnHandResponse> = stockOnHandRepository.getLotBreakdown(organizationId, productId, warehouseId)

    fun getLotGenealogy(
        organizationId: java.util.UUID,
        lotNumber: String,
    ): List<StockMovement> = stockMovementRepository.findByOrganizationIdAndLotNumberOrderByOccurredAtAsc(organizationId, lotNumber)

    fun getPickingSuggestions(
        organizationId: java.util.UUID,
        productId: java.util.UUID,
        warehouseId: java.util.UUID,
        requestedQuantity: BigDecimal,
    ): List<LotOnHandResponse> {
        val product =
            productRepository.findById(productId).orElseThrow {
                ResourceNotFoundException("Product not found")
            }
        if (product.organizationId != organizationId) {
            throw ResourceNotFoundException("Product not found")
        }
        return stockOnHandRepository.getPickingSuggestions(organizationId, productId, warehouseId, requestedQuantity)
    }

    private fun validateLotTracking(
        product: Product,
        lotNumber: String?,
    ): String? {
        if (product.isLotTracked) {
            if (lotNumber.isNullOrBlank()) {
                throw BusinessRuleException("Lot number is required for lot-tracked product '${product.sku}'")
            }
            return lotNumber.trim()
        }
        return lotNumber?.trim()?.takeIf { it.isNotEmpty() }
    }

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
        warehouseId: java.util.UUID,
        transferToWarehouseId: java.util.UUID?,
    ) {
        if (type == StockMovementType.TRANSFER) {
            if (transferToWarehouseId == null) {
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
        warehouseId: java.util.UUID,
        organizationId: java.util.UUID,
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
        lotNumber: String?,
        organizationId: java.util.UUID,
        quantity: BigDecimal,
        sourceWarehouse: Warehouse,
        destWarehouse: Warehouse?,
        sourceLocationId: java.util.UUID?,
        destLocationId: java.util.UUID?,
    ) {
        val sourceDelta = sourceDelta(type, quantity)
        if (sourceDelta.signum() != 0) {
            val ok =
                stockOnHandRepository.applyDelta(
                    organizationId,
                    request.productId,
                    sourceWarehouse.id,
                    sourceLocationId,
                    lotNumber,
                    sourceDelta,
                    allowNegative = sourceWarehouse.allowNegativeStock,
                )
            if (!ok) {
                val current = stockOnHandRepository.get(organizationId, request.productId, sourceWarehouse.id, lotNumber)
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
                destLocationId,
                lotNumber,
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

    private fun validateAndProcessSerials(
        product: Product,
        type: StockMovementType,
        quantity: BigDecimal,
        serialNumbers: List<String>?,
        sourceWarehouseId: java.util.UUID,
        destWarehouseId: java.util.UUID?,
        sourceLocationId: java.util.UUID?,
        destLocationId: java.util.UUID?,
        lotNumber: String?,
        organizationId: java.util.UUID,
    ) {
        if (!product.isSerialized) {
            if (!serialNumbers.isNullOrEmpty()) {
                throw BusinessRuleException("Serial numbers cannot be provided for non-serialized product '${product.sku}'")
            }
            return
        }

        if (serialNumbers.isNullOrEmpty()) {
            throw BusinessRuleException("Serial numbers are required for serialized product '${product.sku}'")
        }

        if (quantity.scale() > 0 && quantity.stripTrailingZeros().scale() > 0) {
            throw BusinessRuleException("Quantity must be a whole number for serialized product '${product.sku}'")
        }

        val requiredCount = quantity.abs().intValueExact()
        if (serialNumbers.size != requiredCount) {
            throw BusinessRuleException("Expected $requiredCount serial numbers but got ${serialNumbers.size}")
        }

        val existingSerials =
            productSerialRepository
                .findByOrganizationIdAndProductIdAndSerialNumberIn(
                    organizationId,
                    product.id,
                    serialNumbers,
                ).associateBy { it.serialNumber }

        when (type) {
            StockMovementType.RECEIPT, StockMovementType.OPENING_BALANCE -> {
                for (sn in serialNumbers) {
                    val existing = existingSerials[sn]
                    if (existing != null && existing.status == SerialStatus.IN_STOCK) {
                        throw BusinessRuleException("Serial number '$sn' is already in stock")
                    }
                    val record =
                        existing ?: ProductSerial(
                            organizationId = organizationId,
                            productId = product.id,
                            serialNumber = sn,
                            status = SerialStatus.IN_STOCK,
                        )
                    record.status = SerialStatus.IN_STOCK
                    record.currentWarehouseId = sourceWarehouseId
                    record.currentLocationId = sourceLocationId
                    record.lotNumber = lotNumber
                    productSerialRepository.save(record)
                }
            }
            StockMovementType.ISSUE, StockMovementType.WIP_ISSUE -> {
                for (sn in serialNumbers) {
                    val existing =
                        existingSerials[sn]
                            ?: throw BusinessRuleException("Serial number '$sn' does not exist")
                    if (existing.status != SerialStatus.IN_STOCK) {
                        throw BusinessRuleException("Serial number '$sn' is not in stock")
                    }
                    if (existing.currentWarehouseId != sourceWarehouseId) {
                        throw BusinessRuleException("Serial number '$sn' is not in warehouse $sourceWarehouseId")
                    }
                    existing.status = SerialStatus.ISSUED
                    existing.currentWarehouseId = null
                    existing.currentLocationId = null
                    productSerialRepository.save(existing)
                }
            }
            StockMovementType.TRANSFER -> {
                for (sn in serialNumbers) {
                    val existing =
                        existingSerials[sn]
                            ?: throw BusinessRuleException("Serial number '$sn' does not exist")
                    if (existing.status != SerialStatus.IN_STOCK) {
                        throw BusinessRuleException("Serial number '$sn' is not in stock")
                    }
                    if (existing.currentWarehouseId != sourceWarehouseId) {
                        throw BusinessRuleException("Serial number '$sn' is not in warehouse $sourceWarehouseId")
                    }
                    existing.currentWarehouseId = destWarehouseId
                    existing.currentLocationId = destLocationId
                    productSerialRepository.save(existing)
                }
            }
            StockMovementType.ADJUSTMENT -> {
                if (quantity.signum() > 0) {
                    for (sn in serialNumbers) {
                        val existing = existingSerials[sn]
                        if (existing != null && existing.status == SerialStatus.IN_STOCK) {
                            throw BusinessRuleException("Serial number '$sn' is already in stock")
                        }
                        val record =
                            existing ?: ProductSerial(
                                organizationId = organizationId,
                                productId = product.id,
                                serialNumber = sn,
                                status = SerialStatus.IN_STOCK,
                            )
                        record.status = SerialStatus.IN_STOCK
                        record.currentWarehouseId = sourceWarehouseId
                        record.currentLocationId = sourceLocationId
                        record.lotNumber = lotNumber
                        productSerialRepository.save(record)
                    }
                } else {
                    for (sn in serialNumbers) {
                        val existing =
                            existingSerials[sn]
                                ?: throw BusinessRuleException("Serial number '$sn' does not exist")
                        if (existing.status != SerialStatus.IN_STOCK) {
                            throw BusinessRuleException("Serial number '$sn' is not in stock")
                        }
                        if (existing.currentWarehouseId != sourceWarehouseId) {
                            throw BusinessRuleException("Serial number '$sn' is not in warehouse $sourceWarehouseId")
                        }
                        existing.status = SerialStatus.ADJUSTED_OUT
                        existing.currentWarehouseId = null
                        existing.currentLocationId = null
                        productSerialRepository.save(existing)
                    }
                }
            }
            StockMovementType.WIP_RECEIPT -> {
                // Similar to RECEIPT
                for (sn in serialNumbers) {
                    val existing = existingSerials[sn]
                    if (existing != null && existing.status == SerialStatus.IN_STOCK) {
                        throw BusinessRuleException("Serial number '$sn' is already in stock")
                    }
                    val record =
                        existing ?: ProductSerial(
                            organizationId = organizationId,
                            productId = product.id,
                            serialNumber = sn,
                            status = SerialStatus.IN_STOCK,
                        )
                    record.status = SerialStatus.IN_STOCK
                    record.currentWarehouseId = sourceWarehouseId
                    record.currentLocationId = sourceLocationId
                    record.lotNumber = lotNumber
                    productSerialRepository.save(record)
                }
            }
        }
    }

    private fun validateAndProcessExpiry(
        product: Product,
        type: StockMovementType,
        lotNumber: String?,
        expiryDate: java.time.LocalDate?,
        organizationId: java.util.UUID,
    ) {
        if (!product.hasExpiry) {
            return
        }

        if (lotNumber.isNullOrBlank()) {
            throw BusinessRuleException("Lot number is required for product '${product.sku}' to track expiry")
        }

        val existingLot =
            productLotRepository.findByOrganizationIdAndProductIdAndLotNumber(
                organizationId,
                product.id,
                lotNumber,
            )

        when (type) {
            StockMovementType.RECEIPT, StockMovementType.OPENING_BALANCE, StockMovementType.WIP_RECEIPT -> {
                if (existingLot != null) {
                    // Lot already exists, optionally we can enforce that expiry matches, but for now we just allow the movement
                    // if expiryDate is provided, update it? Or ignore? Usually expiry is set once.
                    if (expiryDate != null && existingLot.expiryDate != expiryDate) {
                        existingLot.expiryDate = expiryDate
                        productLotRepository.save(existingLot)
                    }
                } else {
                    if (expiryDate == null) {
                        throw BusinessRuleException("Expiry date is required when receiving expiry-tracked product '${product.sku}'")
                    }
                    val newLot =
                        com.aquinofroilan.tessera.domain.inventory.model.ProductLot(
                            organizationId = organizationId,
                            productId = product.id,
                            lotNumber = lotNumber,
                            expiryDate = expiryDate,
                        )
                    productLotRepository.save(newLot)
                }
            }
            StockMovementType.ADJUSTMENT -> {
                if (existingLot == null && expiryDate != null) {
                    val newLot =
                        com.aquinofroilan.tessera.domain.inventory.model.ProductLot(
                            organizationId = organizationId,
                            productId = product.id,
                            lotNumber = lotNumber,
                            expiryDate = expiryDate,
                        )
                    productLotRepository.save(newLot)
                }
            }
            else -> {}
        }
    }
}
