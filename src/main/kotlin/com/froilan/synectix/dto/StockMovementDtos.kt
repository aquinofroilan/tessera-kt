package com.froilan.synectix.dto

import com.froilan.synectix.model.StockMovementType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDateTime

data class CreateStockMovementRequest(
    @field:NotNull(message = "Movement type is required")
    val type: StockMovementType?,
    @field:NotBlank(message = "Product ID is required")
    val productId: String,
    @field:NotBlank(message = "Warehouse ID is required")
    val warehouseId: String,
    val transferToWarehouseId: String? = null,
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
    val id: String,
    val type: StockMovementType,
    val productId: String,
    val warehouseId: String,
    val transferToWarehouseId: String?,
    val quantity: BigDecimal,
    val unitCost: BigDecimal?,
    val reference: String?,
    val notes: String?,
    val occurredAt: String,
    val organizationId: String,
    val createdBy: String,
    val createdAt: String?,
)

data class OnHandResponse(
    val productId: String,
    val warehouseId: String,
    val quantity: BigDecimal,
)
