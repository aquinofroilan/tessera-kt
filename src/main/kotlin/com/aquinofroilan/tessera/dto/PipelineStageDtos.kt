package com.aquinofroilan.tessera.dto
import java.util.UUID

import com.aquinofroilan.tessera.model.PipelineStage
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class CreatePipelineStageRequest(
    @field:NotBlank(message = "Code is required")
    @field:Size(max = 64)
    val code: String,
    @field:NotBlank(message = "Name is required")
    val name: String,
    val description: String? = null,
    val sortOrder: Int? = null,
    @field:DecimalMin("0.00")
    @field:DecimalMax("100.00")
    val probabilityPct: BigDecimal? = null,
    val isWon: Boolean = false,
    val isLost: Boolean = false,
)

data class UpdatePipelineStageRequest(
    val name: String? = null,
    val description: String? = null,
    val sortOrder: Int? = null,
    @field:DecimalMin("0.00")
    @field:DecimalMax("100.00")
    val probabilityPct: BigDecimal? = null,
    val isWon: Boolean? = null,
    val isLost: Boolean? = null,
    val isActive: Boolean? = null,
)

data class PipelineStageResponse(
    val id: UUID,
    val code: String,
    val name: String,
    val description: String?,
    val sortOrder: Int,
    val probabilityPct: BigDecimal,
    val isWon: Boolean,
    val isLost: Boolean,
    val isActive: Boolean,
) {
    companion object {
        fun from(s: PipelineStage) =
            PipelineStageResponse(
                id = s.id,
                code = s.code,
                name = s.name,
                description = s.description,
                sortOrder = s.sortOrder,
                probabilityPct = s.probabilityPct,
                isWon = s.isWon,
                isLost = s.isLost,
                isActive = s.isActive,
            )
    }
}
