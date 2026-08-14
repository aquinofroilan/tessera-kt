package com.aquinofroilan.tessera.domain.assets.dto

import com.aquinofroilan.tessera.domain.assets.model.AssetStatus
import com.aquinofroilan.tessera.domain.assets.model.DepreciationMethod
import com.aquinofroilan.tessera.domain.assets.model.FixedAsset
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class CreateFixedAssetRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(max = 200)
    val name: String,
    @field:Size(max = 1000)
    val description: String? = null,
    val categoryId: String? = null,
    @field:NotNull(message = "Acquisition date is required")
    var acquisitionDate: LocalDate?,
    @field:NotNull(message = "Acquisition cost is required")
    @field:PositiveOrZero(message = "Acquisition cost must be zero or positive")
    var acquisitionCost: BigDecimal?,
    @field:PositiveOrZero(message = "Salvage value must be zero or positive")
    val salvageValue: BigDecimal = BigDecimal.ZERO,
    @field:NotNull(message = "Useful life is required")
    @field:Positive(message = "Useful life must be positive")
    var usefulLifeMonths: Int?,
    val depreciationMethod: DepreciationMethod = DepreciationMethod.STRAIGHT_LINE,
    @field:Size(max = 200)
    val location: String? = null,
    @field:Size(max = 100)
    val serialNumber: String? = null,
    val assetAccountId: String? = null,
    val accumulatedDepreciationAccountId: String? = null,
    val depreciationExpenseAccountId: String? = null,
)

data class UpdateFixedAssetRequest(
    @field:Size(max = 200)
    val name: String? = null,
    @field:Size(max = 1000)
    val description: String? = null,
    val categoryId: String? = null,
    @field:Size(max = 200)
    val location: String? = null,
    @field:Size(max = 100)
    val serialNumber: String? = null,
    val assetAccountId: String? = null,
    val accumulatedDepreciationAccountId: String? = null,
    val depreciationExpenseAccountId: String? = null,
)

data class FixedAssetResponse(
    val id: UUID,
    val assetNumber: String,
    val name: String,
    val description: String?,
    val categoryId: String?,
    val acquisitionDate: String,
    val acquisitionCost: BigDecimal,
    val salvageValue: BigDecimal,
    val usefulLifeMonths: Int,
    val depreciationMethod: DepreciationMethod,
    val location: String?,
    val serialNumber: String?,
    val status: AssetStatus,
    val accumulatedDepreciation: BigDecimal,
    val netBookValue: BigDecimal,
    val assetAccountId: String?,
    val accumulatedDepreciationAccountId: String?,
    val depreciationExpenseAccountId: String?,
    val organizationId: UUID,
    val createdAt: String?,
    val updatedAt: String?,
) {
    companion object {
        fun from(asset: FixedAsset) =
            FixedAssetResponse(
                id = asset.id,
                assetNumber = asset.assetNumber,
                name = asset.name,
                description = asset.description,
                categoryId = asset.categoryId?.toString(),
                acquisitionDate = asset.acquisitionDate.toString(),
                acquisitionCost = asset.acquisitionCost,
                salvageValue = asset.salvageValue,
                usefulLifeMonths = asset.usefulLifeMonths,
                depreciationMethod = asset.depreciationMethod,
                location = asset.location,
                serialNumber = asset.serialNumber,
                status = asset.status,
                accumulatedDepreciation = asset.accumulatedDepreciation,
                netBookValue = asset.acquisitionCost.minus(asset.accumulatedDepreciation),
                assetAccountId = asset.assetAccountId,
                accumulatedDepreciationAccountId = asset.accumulatedDepreciationAccountId,
                depreciationExpenseAccountId = asset.depreciationExpenseAccountId,
                organizationId = asset.organizationId,
                createdAt = asset.createdAt?.toString(),
                updatedAt = (asset.updatedAt ?: asset.createdAt)?.toString(),
            )
    }
}
