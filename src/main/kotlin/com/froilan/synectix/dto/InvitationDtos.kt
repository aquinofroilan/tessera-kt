package com.froilan.synectix.dto

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
    @field:NotBlank(message = "Username is required")
    @field:Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    val username: String,
    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    val password: String,
    @field:NotBlank(message = "First name is required")
    val firstName: String,
    @field:NotBlank(message = "Last name is required")
    val lastName: String,
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
