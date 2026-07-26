package com.aquinofroilan.tessera.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class CreateVendorRequest(
    @field:NotBlank(message = "Vendor name is required")
    val name: String,
    val contactName: String? = null,
    @field:Email(message = "Invalid email format")
    val contactEmail: String? = null,
    val contactPhone: String? = null,
    @field:Min(value = 0, message = "Payment term days must be zero or positive")
    val paymentTermDays: Int = 30,
    val defaultExpenseAccountId: java.util.UUID? = null,
)

data class UpdateVendorRequest(
    val name: String? = null,
    val contactName: String? = null,
    @field:Email(message = "Invalid email format")
    val contactEmail: String? = null,
    val contactPhone: String? = null,
    @field:Min(value = 0, message = "Payment term days must be zero or positive")
    val paymentTermDays: Int? = null,
    val defaultExpenseAccountId: java.util.UUID? = null,
)

data class VendorResponse(
    val id: java.util.UUID,
    val name: String,
    val contactName: String?,
    val contactEmail: String?,
    val contactPhone: String?,
    val paymentTermDays: Int,
    val defaultExpenseAccountId: java.util.UUID?,
    val organizationId: java.util.UUID,
    val isActive: Boolean,
    val createdAt: String?,
    val updatedAt: String?,
)
