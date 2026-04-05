package com.froilan.synectix.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import java.time.LocalDateTime

data class CreateApiKeyRequest(
    @field:NotBlank(message = "API key name is required")
    val name: String,
    @field:NotEmpty(message = "At least one permission is required")
    val permissions: List<
        @NotBlank(message = "Permission must not be blank")
        String,
    >,
    val expiresAt: LocalDateTime? = null,
)

data class ApiKeyResponse(
    val id: String,
    val name: String,
    val keyPrefix: String,
    val permissions: List<String>,
    val organizationId: String,
    val isActive: Boolean,
    val lastUsedAt: String?,
    val expiresAt: String?,
    val createdAt: String?,
)

data class ApiKeyCreatedResponse(
    val id: String,
    val name: String,
    val rawKey: String,
    val keyPrefix: String,
    val permissions: List<String>,
    val organizationId: String,
    val expiresAt: String?,
    val createdAt: String?,
)
