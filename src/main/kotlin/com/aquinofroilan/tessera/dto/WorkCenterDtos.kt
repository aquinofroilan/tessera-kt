package com.aquinofroilan.tessera.dto

import com.aquinofroilan.tessera.model.WorkCenter
import com.aquinofroilan.tessera.model.WorkCenterType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class CreateWorkCenterRequest(
    @field:NotBlank(message = "Code is required")
    @field:Size(max = 64)
    val code: String,
    @field:NotBlank(message = "Name is required")
    val name: String,
    val description: String? = null,
    val type: WorkCenterType = WorkCenterType.MACHINE,
    val warehouseId: String? = null,
    @field:Positive(message = "Capacity must be positive")
    val capacityPerHour: BigDecimal? = null,
    @field:PositiveOrZero(message = "Cost cannot be negative")
    val costPerHour: BigDecimal? = null,
    @field:Positive(message = "Efficiency must be positive")
    val efficiencyPct: BigDecimal? = null,
)

data class UpdateWorkCenterRequest(
    val name: String? = null,
    val description: String? = null,
    val type: WorkCenterType? = null,
    val warehouseId: String? = null,
    @field:Positive
    val capacityPerHour: BigDecimal? = null,
    @field:PositiveOrZero
    val costPerHour: BigDecimal? = null,
    @field:Positive
    val efficiencyPct: BigDecimal? = null,
    val isActive: Boolean? = null,
)

data class WorkCenterResponse(
    val id: String,
    val code: String,
    val name: String,
    val description: String?,
    val type: WorkCenterType,
    val warehouseId: String?,
    val capacityPerHour: BigDecimal,
    val costPerHour: BigDecimal,
    val efficiencyPct: BigDecimal,
    val isActive: Boolean,
) {
    companion object {
        fun from(wc: WorkCenter) =
            WorkCenterResponse(
                id = wc.id,
                code = wc.code,
                name = wc.name,
                description = wc.description,
                type = wc.type,
                warehouseId = wc.warehouseId,
                capacityPerHour = wc.capacityPerHour,
                costPerHour = wc.costPerHour,
                efficiencyPct = wc.efficiencyPct,
                isActive = wc.isActive,
            )
    }
}
