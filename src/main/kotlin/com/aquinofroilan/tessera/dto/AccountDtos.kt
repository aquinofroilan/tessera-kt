package com.aquinofroilan.tessera.dto

import jakarta.validation.constraints.NotBlank

data class CreateAccountRequest(
    @field:NotBlank(message = "Account code is required")
    val code: String,
    @field:NotBlank(message = "Account name is required")
    val name: String,
    @field:NotBlank(message = "Account type is required")
    val type: String,
    val parentId: java.util.UUID? = null,
    val description: String? = null,
)

data class UpdateAccountRequest(
    val name: String? = null,
    val description: String? = null,
)

data class AccountResponse(
    val id: java.util.UUID,
    val code: String,
    val name: String,
    val description: String?,
    val type: String,
    val parentId: java.util.UUID?,
    val organizationId: java.util.UUID,
    val isActive: Boolean,
    val isSystemAccount: Boolean,
    val createdAt: String?,
    val updatedAt: String?,
)
