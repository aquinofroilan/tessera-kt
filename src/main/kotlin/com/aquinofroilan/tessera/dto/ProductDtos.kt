package com.aquinofroilan.tessera.dto

import java.util.UUID

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class CreateProductRequest(
    @field:NotBlank(message = "SKU is required")
    @field:Size(max = 64, message = "SKU must be 64 characters or fewer")
    val sku: String,
    @field:NotBlank(message = "Product name is required")
    @field:Size(max = 255, message = "Product name must be 255 characters or fewer")
    val name: String,
    @field:Size(max = 2000, message = "Description must be 2000 characters or fewer")
    val description: String? = null,
    @field:Size(max = 128, message = "Category must be 128 characters or fewer")
    val category: String? = null,
    @field:Size(max = 2048, message = "Image URL must be 2048 characters or fewer")
    val imageUrl: String? = null,
    @field:DecimalMin(value = "0.0", message = "List price must be zero or positive")
    val listPrice: BigDecimal,
    val priceCurrency: String? = null,
    val taxGroupId: java.util.UUID? = null,
)

data class UpdateProductRequest(
    @field:Size(max = 255, message = "Product name must be 255 characters or fewer")
    val name: String? = null,
    @field:Size(max = 2000, message = "Description must be 2000 characters or fewer")
    val description: String? = null,
    @field:Size(max = 128, message = "Category must be 128 characters or fewer")
    val category: String? = null,
    @field:Size(max = 2048, message = "Image URL must be 2048 characters or fewer")
    val imageUrl: String? = null,
    @field:DecimalMin(value = "0.0", message = "List price must be zero or positive")
    val listPrice: BigDecimal? = null,
    val priceCurrency: String? = null,
    val taxGroupId: java.util.UUID? = null,
)

data class ProductResponse(
    val id: java.util.UUID,
    val sku: String,
    val name: String,
    val description: String?,
    val category: String?,
    val imageUrl: String?,
    val listPrice: BigDecimal,
    val priceCurrency: String,
    val taxGroupId: java.util.UUID?,
    val organizationId: java.util.UUID,
    val isActive: Boolean,
    val createdAt: String?,
    val updatedAt: String?,
)
