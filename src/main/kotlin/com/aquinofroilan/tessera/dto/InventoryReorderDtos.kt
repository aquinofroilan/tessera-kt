package com.aquinofroilan.tessera.dto

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

data class CreateReorderRuleRequest(
    @field:NotNull(message = "Product ID is required")
    val productId: java.util.UUID,
    @field:NotNull(message = "Warehouse ID is required")
    val warehouseId: java.util.UUID,
    @field:NotNull(message = "Reorder point is required")
    @field:DecimalMin(value = "0.0", message = "Reorder point must be zero or positive")
    val reorderPoint: BigDecimal?,
    @field:DecimalMin(value = "0.0", message = "Safety stock must be zero or positive")
    val safetyStock: BigDecimal? = null,
)

data class UpdateReorderRuleRequest(
    @field:DecimalMin(value = "0.0", message = "Reorder point must be zero or positive")
    val reorderPoint: BigDecimal? = null,
    @field:DecimalMin(value = "0.0", message = "Safety stock must be zero or positive")
    val safetyStock: BigDecimal? = null,
)

data class ReorderRuleResponse(
    val id: java.util.UUID,
    val productId: java.util.UUID,
    val warehouseId: java.util.UUID,
    val reorderPoint: BigDecimal,
    val safetyStock: BigDecimal,
    val organizationId: java.util.UUID,
    val createdAt: String?,
    val updatedAt: String?,
)

data class LowStockLineResponse(
    val productId: java.util.UUID,
    val warehouseId: java.util.UUID,
    val onHand: BigDecimal,
    val reorderPoint: BigDecimal,
    val safetyStock: BigDecimal,
    val shortfall: BigDecimal,
)

data class LowStockReportResponse(
    val lines: List<LowStockLineResponse>,
)
