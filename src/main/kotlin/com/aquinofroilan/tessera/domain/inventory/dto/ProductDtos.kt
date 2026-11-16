package com.aquinofroilan.tessera.domain.inventory.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSetter
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.util.UUID

data class CreateProductRequest(
    @field:NotBlank(message = "SKU is required")
    @field:Size(max = 64, message = "SKU must be 64 characters or fewer")
    val sku: String,
    @field:NotBlank(message = "Product name is required")
    @field:Size(max = 255, message = "Product name must be 255 characters or fewer")
    val name: String,
    @field:Size(max = 2000, message = "Description must be 2000 characters or fewer")
    val description: String? = null,
    val categoryId: UUID? = null,
    @field:Size(max = 2048, message = "Image URL must be 2048 characters or fewer")
    val imageUrl: String? = null,
    @field:DecimalMin(value = "0.0", message = "List price must be zero or positive")
    val listPrice: BigDecimal,
    val priceCurrency: String? = null,
    val taxGroupId: java.util.UUID? = null,
    @param:JsonProperty("isLotTracked")
    @get:JsonProperty("isLotTracked")
    val isLotTracked: Boolean? = false,
    @param:JsonProperty("isSerialized")
    @get:JsonProperty("isSerialized")
    val isSerialized: Boolean? = false,
    val hasExpiry: Boolean? = false,
    @field:DecimalMin(value = "0.0", message = "Unit weight must be zero or positive")
    val unitWeightKg: BigDecimal? = null,
    @field:DecimalMin(value = "0.0", message = "Unit volume must be zero or positive")
    val unitVolumeCbm: BigDecimal? = null,
)

class UpdateProductRequest(
    @field:Size(max = 255, message = "Product name must be 255 characters or fewer")
    val name: String? = null,
    @field:Size(max = 2000, message = "Description must be 2000 characters or fewer")
    val description: String? = null,
    @field:Size(max = 2048, message = "Image URL must be 2048 characters or fewer")
    val imageUrl: String? = null,
    @field:DecimalMin(value = "0.0", message = "List price must be zero or positive")
    val listPrice: BigDecimal? = null,
    val priceCurrency: String? = null,
    val taxGroupId: java.util.UUID? = null,
    @param:JsonProperty("isLotTracked")
    @get:JsonProperty("isLotTracked")
    val isLotTracked: Boolean? = null,
    @param:JsonProperty("isSerialized")
    @get:JsonProperty("isSerialized")
    val isSerialized: Boolean? = null,
    val hasExpiry: Boolean? = null,
    @field:DecimalMin(value = "0.0", message = "Unit weight must be zero or positive")
    val unitWeightKg: BigDecimal? = null,
    @field:DecimalMin(value = "0.0", message = "Unit volume must be zero or positive")
    val unitVolumeCbm: BigDecimal? = null,
) {
    var categoryId: UUID? = null
        private set

    @get:JsonIgnore
    var categoryIdProvided: Boolean = false
        private set

    @JsonSetter("categoryId")
    fun assignCategoryId(value: UUID?) {
        categoryId = value
        categoryIdProvided = true
    }
}

data class ProductResponse(
    val id: java.util.UUID,
    val sku: String,
    val name: String,
    val description: String?,
    val categoryId: UUID?,
    val imageUrl: String?,
    val listPrice: BigDecimal,
    val priceCurrency: String,
    val taxGroupId: java.util.UUID?,
    val organizationId: java.util.UUID,
    @param:JsonProperty("isLotTracked")
    @get:JsonProperty("isLotTracked")
    val isLotTracked: Boolean,
    @param:JsonProperty("isSerialized")
    @get:JsonProperty("isSerialized")
    val isSerialized: Boolean,
    val hasExpiry: Boolean,
    @param:JsonProperty("isActive")
    @get:JsonProperty("isActive")
    val isActive: Boolean,
    val unitWeightKg: BigDecimal?,
    val unitVolumeCbm: BigDecimal?,
    val createdAt: String?,
    val updatedAt: String?,
)
