package com.aquinofroilan.tessera.dto

import com.aquinofroilan.tessera.model.StockMovementType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDateTime

data class CreateStockMovementRequest(
    @field:NotNull(message = "Movement type is required")
    val type: StockMovementType?,
    @field:NotNull(message = "Product ID is required")
    val productId: java.util.UUID,
    @field:NotNull(message = "Warehouse ID is required")
    val warehouseId: java.util.UUID,
    val transferToWarehouseId: java.util.UUID? = null,
    @field:NotNull(message = "Quantity is required")
    val quantity: BigDecimal?,
    val unitCost: BigDecimal? = null,
    @field:Size(max = 128, message = "Reference must be 128 characters or fewer")
    val reference: String? = null,
    @field:Size(max = 2000, message = "Notes must be 2000 characters or fewer")
    val notes: String? = null,
    val occurredAt: LocalDateTime? = null,
)

data class StockMovementResponse(
    val id: java.util.UUID,
    val type: StockMovementType,
    val productId: java.util.UUID,
    val warehouseId: java.util.UUID,
    val transferToWarehouseId: java.util.UUID?,
    val quantity: BigDecimal,
    val unitCost: BigDecimal?,
    val reference: String?,
    val notes: String?,
    val occurredAt: String,
    val organizationId: java.util.UUID,
    val createdBy: java.util.UUID,
    val createdAt: String?,
)

data class OnHandResponse(
    val productId: java.util.UUID,
    val warehouseId: java.util.UUID,
    val quantity: BigDecimal,
)
