package com.aquinofroilan.tessera.dto

import com.aquinofroilan.tessera.model.ProductStandardCost
import jakarta.validation.constraints.PositiveOrZero
import java.math.BigDecimal
import java.time.LocalDateTime

data class RollupRequest(
    val bomId: java.util.UUID? = null,
    val routingId: java.util.UUID? = null,
    @field:PositiveOrZero(message = "Overhead rate cannot be negative")
    val overheadRatePct: BigDecimal? = null,
    val notes: String? = null,
)

data class ManualStandardCostRequest(
    @field:PositiveOrZero
    val materialCost: BigDecimal? = null,
    @field:PositiveOrZero
    val laborCost: BigDecimal? = null,
    @field:PositiveOrZero
    val overheadCost: BigDecimal? = null,
    val notes: String? = null,
)

data class StandardCostResponse(
    val id: java.util.UUID,
    val productId: java.util.UUID,
    val bomId: java.util.UUID?,
    val routingId: java.util.UUID?,
    val materialCost: BigDecimal,
    val laborCost: BigDecimal,
    val overheadCost: BigDecimal,
    val totalCost: BigDecimal,
    val source: String,
    val calculatedAt: LocalDateTime,
    val calculatedBy: String,
    val notes: String?,
) {
    companion object {
        fun from(c: ProductStandardCost) =
            StandardCostResponse(
                id = c.id,
                productId = c.productId,
                bomId = c.bomId,
                routingId = c.routingId,
                materialCost = c.materialCost,
                laborCost = c.laborCost,
                overheadCost = c.overheadCost,
                totalCost = c.totalCost,
                source = c.source,
                calculatedAt = c.calculatedAt,
                calculatedBy = c.calculatedBy,
                notes = c.notes,
            )
    }
}
