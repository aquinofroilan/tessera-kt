package com.loom.synectix.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateInvitationRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email must be valid")
    val email: String,
    @field:NotBlank(message = "Role is required")
    val role: String,
)

data class AcceptInvitationRequest(
    @field:NotBlank(message = "Invitation token is required")
    val token: String,
    @field:Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    val username: String? = null,
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    val password: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
)

data class ValidateInvitationRequest(
    @field:NotBlank(message = "Invitation token is required")
    val token: String,
)

data class ValidateInvitationResponse(
    val email: String,
    val role: String,
    val organizationId: String,
    val existingUser: Boolean,
)

data class InvitationResponse(
    val id: String,
    val email: String,
    val role: String,
    val status: String,
    val invitedBy: String,
    val expiresAt: String,
    val createdAt: String?,
)
