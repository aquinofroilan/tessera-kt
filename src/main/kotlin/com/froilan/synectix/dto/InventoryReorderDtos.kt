package com.froilan.synectix.dto

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

data class CreateReorderRuleRequest(
    @field:NotBlank(message = "Product ID is required")
    val productId: String,
    @field:NotBlank(message = "Warehouse ID is required")
    val warehouseId: String,
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
    val id: String,
    val productId: String,
    val warehouseId: String,
    val reorderPoint: BigDecimal,
    val safetyStock: BigDecimal,
    val organizationId: String,
    val createdAt: String?,
    val updatedAt: String?,
)

data class LowStockLineResponse(
    val productId: String,
    val warehouseId: String,
    val onHand: BigDecimal,
    val reorderPoint: BigDecimal,
    val safetyStock: BigDecimal,
    val shortfall: BigDecimal,
)

data class LowStockReportResponse(
    val lines: List<LowStockLineResponse>,
)
