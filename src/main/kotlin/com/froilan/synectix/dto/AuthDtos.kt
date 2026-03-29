package com.froilan.synectix.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class LoginRequest(
    @field:NotBlank(message = "Username is required")
    val username: String,
    @field:NotBlank(message = "Password is required")
    val password: String,
)

data class RegisterRequest(
    @field:NotBlank(message = "Username is required")
    @field:Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    val username: String,
    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    val password: String,
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email must be valid")
    val email: String,
    @field:NotBlank(message = "First name is required")
    val firstName: String,
    @field:NotBlank(message = "Last name is required")
    val lastName: String,
    @field:NotBlank(message = "Organization name is required")
    val orgName: String,
    @field:NotBlank(message = "Organization slug is required")
    val orgSlug: String,
    val orgDescription: String? = null,
    @field:NotBlank(message = "Organization base currency is required")
    val orgBaseCurrency: String,
    val orgFiscalYearStart: LocalDateTime,
    @field:NotBlank(message = "Organization timezone is required")
    val orgTimezone: String,
    @field:NotBlank(message = "Organization legal name is required")
    val orgLegalName: String,
    @field:NotBlank(message = "Organization trade name is required")
    val orgTradeName: String,
)

data class RefreshRequest(
    @field:NotBlank(message = "Refresh token is required")
    val refreshToken: String,
)

data class ChangePasswordRequest(
    @field:NotBlank(message = "Current password is required")
    val currentPassword: String,
    @field:NotBlank(message = "New password is required")
    @field:Size(min = 8, message = "New password must be at least 8 characters")
    val newPassword: String,
)

data class ForgotPasswordRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email must be valid")
    val email: String,
)

data class ResetPasswordRequest(
    @field:NotBlank(message = "Reset token is required")
    val token: String,
    @field:NotBlank(message = "New password is required")
    @field:Size(min = 8, message = "New password must be at least 8 characters")
    val newPassword: String,
)

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val username: String,
    val roles: List<String>,
    val expiresAt: String,
    val refreshTokenExpiresAt: String,
)
