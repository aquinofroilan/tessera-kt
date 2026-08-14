package com.aquinofroilan.tessera.domain.mfg.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import java.math.BigDecimal

data class IssueMaterialLineRequest(
    @field:NotBlank(message = "Component line ID is required")
    val componentLineId: java.util.UUID,
    @field:NotNull(message = "Quantity is required")
    @field:Positive(message = "Quantity must be positive")
    val quantity: BigDecimal?,
)

data class IssueMaterialRequest(
    @field:NotEmpty(message = "At least one component line is required")
    @field:Valid
    val lines: List<IssueMaterialLineRequest>,
    val notes: String? = null,
)

data class CompleteWorkOrderRequest(
    @field:NotNull(message = "Quantity completed is required")
    @field:PositiveOrZero(message = "Quantity completed cannot be negative")
    val quantityCompleted: BigDecimal?,
    @field:PositiveOrZero(message = "Quantity scrapped cannot be negative")
    val quantityScrapped: BigDecimal? = null,
    @field:PositiveOrZero(message = "Unit cost override cannot be negative")
    val unitCostOverride: BigDecimal? = null,
    val notes: String? = null,
)
