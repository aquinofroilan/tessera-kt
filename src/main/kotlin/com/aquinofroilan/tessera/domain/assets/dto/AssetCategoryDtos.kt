package com.aquinofroilan.tessera.domain.assets.dto

import com.aquinofroilan.tessera.domain.assets.model.AssetCategory
import com.aquinofroilan.tessera.domain.assets.model.DepreciationMethod
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.util.UUID

data class CreateAssetCategoryRequest(
    @field:NotBlank(message = "Code is required")
    @field:Size(max = 32)
    val code: String,
    @field:NotBlank(message = "Name is required")
    @field:Size(max = 120)
    val name: String,
    @field:Size(max = 500)
    val description: String? = null,
    @field:Positive(message = "Useful life must be positive")
    val defaultUsefulLifeMonths: Int? = null,
    val defaultDepreciationMethod: DepreciationMethod = DepreciationMethod.STRAIGHT_LINE,
    @field:PositiveOrZero(message = "Salvage value must be zero or positive")
    val defaultSalvageValue: BigDecimal = BigDecimal.ZERO,
)

data class UpdateAssetCategoryRequest(
    @field:Size(max = 120)
    val name: String? = null,
    @field:Size(max = 500)
    val description: String? = null,
    @field:Positive
    val defaultUsefulLifeMonths: Int? = null,
    val defaultDepreciationMethod: DepreciationMethod? = null,
    @field:PositiveOrZero
    val defaultSalvageValue: BigDecimal? = null,
    val isActive: Boolean? = null,
)

data class AssetCategoryResponse(
    val id: UUID,
    val code: String,
    val name: String,
    val description: String?,
    val defaultUsefulLifeMonths: Int?,
    val defaultDepreciationMethod: DepreciationMethod,
    val defaultSalvageValue: BigDecimal,
    val isActive: Boolean,
    val organizationId: UUID,
    val createdAt: String?,
    val updatedAt: String?,
) {
    companion object {
        fun from(category: AssetCategory) =
            AssetCategoryResponse(
                id = category.id,
                code = category.code,
                name = category.name,
                description = category.description,
                defaultUsefulLifeMonths = category.defaultUsefulLifeMonths,
                defaultDepreciationMethod = category.defaultDepreciationMethod,
                defaultSalvageValue = category.defaultSalvageValue,
                isActive = category.isActive,
                organizationId = category.organizationId,
                createdAt = category.createdAt?.toString(),
                updatedAt = (category.updatedAt ?: category.createdAt)?.toString(),
            )
    }
}
