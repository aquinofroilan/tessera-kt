package com.aquinofroilan.tessera.domain.inventory.dto

import com.aquinofroilan.tessera.domain.inventory.model.ProductCategory
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

data class CreateProductCategoryRequest(
    @field:NotBlank @field:Size(max = 128) val name: String,
    @field:Size(max = 2000) val description: String? = null,
)

data class UpdateProductCategoryRequest(
    @field:NotBlank @field:Size(max = 128) val name: String? = null,
    @field:Size(max = 2000) val description: String? = null,
    val isActive: Boolean? = null,
)

data class ProductCategoryResponse(
    val id: UUID,
    val name: String,
    val description: String?,
    val isActive: Boolean,
) {
    companion object {
        fun from(category: ProductCategory) = ProductCategoryResponse(category.id, category.name, category.description, category.isActive)
    }
}
