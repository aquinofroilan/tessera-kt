package com.aquinofroilan.tessera.dto

import com.aquinofroilan.tessera.model.ProductVariant
import com.aquinofroilan.tessera.model.UnitOfMeasure
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class CreateUomRequest(
    @field:NotBlank(message = "Code is required")
    @field:Size(max = 32)
    val code: String,
    @field:NotBlank(message = "Name is required")
    val name: String,
    val description: String? = null,
    val baseUomId: String? = null,
    @field:Positive(message = "Conversion factor must be positive")
    val conversionFactor: BigDecimal? = null,
)

data class UpdateUomRequest(
    val name: String? = null,
    val description: String? = null,
    val baseUomId: String? = null,
    @field:Positive
    val conversionFactor: BigDecimal? = null,
    val isActive: Boolean? = null,
)

data class UomResponse(
    val id: String,
    val code: String,
    val name: String,
    val description: String?,
    val baseUomId: String?,
    val conversionFactor: BigDecimal,
    val isActive: Boolean,
) {
    companion object {
        fun from(u: UnitOfMeasure) =
            UomResponse(
                id = u.id,
                code = u.code,
                name = u.name,
                description = u.description,
                baseUomId = u.baseUomId,
                conversionFactor = u.conversionFactor,
                isActive = u.isActive,
            )
    }
}

data class CreateProductVariantRequest(
    @field:NotBlank(message = "Code is required")
    val code: String,
    @field:NotBlank(message = "Name is required")
    val name: String,
    val skuSuffix: String? = null,
    val attributes: Map<String, Any> = emptyMap(),
)

data class UpdateProductVariantRequest(
    val name: String? = null,
    val skuSuffix: String? = null,
    val attributes: Map<String, Any>? = null,
    val isActive: Boolean? = null,
)

data class ProductVariantResponse(
    val id: String,
    val productId: String,
    val code: String,
    val name: String,
    val skuSuffix: String?,
    val attributes: Map<String, Any>,
    val isActive: Boolean,
) {
    companion object {
        fun from(v: ProductVariant) =
            ProductVariantResponse(
                id = v.id,
                productId = v.productId,
                code = v.code,
                name = v.name,
                skuSuffix = v.skuSuffix,
                attributes = v.attributes,
                isActive = v.isActive,
            )
    }
}
