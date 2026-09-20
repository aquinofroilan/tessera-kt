package com.aquinofroilan.tessera.domain.inventory.service

import com.aquinofroilan.tessera.domain.inventory.dto.PlaceHoldRequest
import com.aquinofroilan.tessera.domain.inventory.model.HoldStatus
import com.aquinofroilan.tessera.domain.inventory.model.InventoryHold
import com.aquinofroilan.tessera.domain.inventory.model.SerialStatus
import com.aquinofroilan.tessera.domain.inventory.repository.InventoryHoldRepository
import com.aquinofroilan.tessera.domain.inventory.repository.ProductRepository
import com.aquinofroilan.tessera.domain.inventory.repository.ProductSerialRepository
import com.aquinofroilan.tessera.domain.inventory.repository.StockOnHandQueries
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Service
class InventoryHoldService(
    private val inventoryHoldRepository: InventoryHoldRepository,
    private val productRepository: ProductRepository,
    private val productSerialRepository: ProductSerialRepository,
    private val stockOnHandQueries: StockOnHandQueries,
) {
    @Transactional
    fun placeOnHold(
        request: PlaceHoldRequest,
        organizationId: UUID,
        userId: UUID,
    ): InventoryHold {
        val product =
            productRepository.findById(request.productId).orElseThrow {
                ResourceNotFoundException("Product not found")
            }
        if (product.organizationId != organizationId) {
            throw ResourceNotFoundException("Product not found")
        }

        val resolvedLot = request.lotNumber ?: ""

        val quantity =
            if (product.isSerialized) {
                if (request.serialNumbers.isNullOrEmpty()) {
                    throw BusinessRuleException("Serial numbers must be provided to hold serialized product")
                }
                BigDecimal(request.serialNumbers.size)
            } else {
                if (request.quantity == null || request.quantity.signum() <= 0) {
                    throw BusinessRuleException("A positive quantity must be provided")
                }
                if (!request.serialNumbers.isNullOrEmpty()) {
                    throw BusinessRuleException("Cannot hold specific serial numbers for a non-serialized product")
                }
                request.quantity
            }

        if (product.isLotTracked && resolvedLot.isEmpty()) {
            throw BusinessRuleException("Lot number must be provided to hold lot-tracked product")
        }

        if (product.isSerialized) {
            val serialNumbers = request.serialNumbers!!
            val existingSerials =
                productSerialRepository
                    .findByOrganizationIdAndProductIdAndSerialNumberIn(
                        organizationId,
                        product.id,
                        serialNumbers,
                    ).associateBy { it.serialNumber }

            for (sn in serialNumbers) {
                val existing = existingSerials[sn] ?: throw BusinessRuleException("Serial number '$sn' not found")
                if (existing.currentWarehouseId != request.warehouseId) {
                    throw BusinessRuleException("Serial number '$sn' is not in warehouse '${request.warehouseId}'")
                }
                if (existing.status != SerialStatus.IN_STOCK) {
                    throw BusinessRuleException("Serial number '$sn' cannot be put on hold because its status is '${existing.status}'")
                }
                existing.status = SerialStatus.QUARANTINED
            }
            productSerialRepository.saveAll(existingSerials.values)
        }

        val ok =
            stockOnHandQueries.applyHold(
                organizationId = organizationId,
                productId = product.id,
                warehouseId = request.warehouseId,
                lotNumber = resolvedLot,
                delta = quantity,
            )

        if (!ok) {
            throw BusinessRuleException("Insufficient available stock to place on hold")
        }

        val hold =
            InventoryHold(
                organizationId = organizationId,
                productId = product.id,
                warehouseId = request.warehouseId,
                lotNumber = resolvedLot,
                serialNumbers = request.serialNumbers,
                quantity = quantity,
                reference = request.reference,
                notes = request.notes,
                status = HoldStatus.ACTIVE,
                createdBy = userId,
            )
        return inventoryHoldRepository.save(hold)
    }

    @Transactional
    fun releaseHold(
        holdId: UUID,
        organizationId: UUID,
        userId: UUID,
    ): InventoryHold {
        val hold =
            inventoryHoldRepository.findById(holdId).orElseThrow {
                ResourceNotFoundException("Inventory hold not found")
            }
        if (hold.organizationId != organizationId) {
            throw ResourceNotFoundException("Inventory hold not found")
        }
        if (hold.status != HoldStatus.ACTIVE) {
            throw BusinessRuleException("Inventory hold is already released")
        }

        if (!hold.serialNumbers.isNullOrEmpty()) {
            val existingSerials =
                productSerialRepository
                    .findByOrganizationIdAndProductIdAndSerialNumberIn(
                        organizationId,
                        hold.productId,
                        hold.serialNumbers!!,
                    )
            for (serial in existingSerials) {
                if (serial.status == SerialStatus.QUARANTINED) {
                    serial.status = SerialStatus.IN_STOCK
                }
            }
            productSerialRepository.saveAll(existingSerials)
        }

        val ok =
            stockOnHandQueries.applyHold(
                organizationId = organizationId,
                productId = hold.productId,
                warehouseId = hold.warehouseId,
                lotNumber = hold.lotNumber,
                delta = hold.quantity.negate(),
            )
        if (!ok) {
            throw BusinessRuleException("Failed to release held stock from on-hand balances")
        }

        hold.status = HoldStatus.RELEASED
        hold.releasedAt = LocalDateTime.now()
        hold.releasedBy = userId

        return inventoryHoldRepository.save(hold)
    }
}
