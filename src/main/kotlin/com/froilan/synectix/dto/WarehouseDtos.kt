package com.froilan.synectix.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateWarehouseRequest(
    @field:NotBlank(message = "Warehouse code is required")
    @field:Size(max = 64, message = "Warehouse code must be 64 characters or fewer")
    val code: String,
    @field:NotBlank(message = "Warehouse name is required")
    @field:Size(max = 255, message = "Warehouse name must be 255 characters or fewer")
    val name: String,
    @field:Size(max = 2000, message = "Description must be 2000 characters or fewer")
    val description: String? = null,
    @field:Size(max = 255, message = "Address line must be 255 characters or fewer")
    val addressLine: String? = null,
    @field:Size(max = 128, message = "City must be 128 characters or fewer")
    val city: String? = null,
    @field:Size(max = 64, message = "Country must be 64 characters or fewer")
    val country: String? = null,
    val allowNegativeStock: Boolean? = null,
)

data class UpdateWarehouseRequest(
    @field:Size(max = 255, message = "Warehouse name must be 255 characters or fewer")
    val name: String? = null,
    @field:Size(max = 2000, message = "Description must be 2000 characters or fewer")
    val description: String? = null,
    @field:Size(max = 255, message = "Address line must be 255 characters or fewer")
    val addressLine: String? = null,
    @field:Size(max = 128, message = "City must be 128 characters or fewer")
    val city: String? = null,
    @field:Size(max = 64, message = "Country must be 64 characters or fewer")
    val country: String? = null,
    val allowNegativeStock: Boolean? = null,
)

data class WarehouseResponse(
    val id: String,
    val code: String,
    val name: String,
    val description: String?,
    val addressLine: String?,
    val city: String?,
    val country: String?,
    val allowNegativeStock: Boolean,
    val organizationId: String,
    val isActive: Boolean,
    val createdAt: String?,
    val updatedAt: String?,
)
