package com.aquinofroilan.tessera.domain.platform.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class RequestLoginLinkRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String,
)

data class ConsumeLoginLinkRequest(
    @field:NotBlank(message = "Token is required")
    val token: String,
)

/**
 * Returned only in dev / test convenience flows. In production the raw
 * token is delivered out-of-band via email; the request endpoint returns
 * a generic message regardless of whether the email exists.
 */
data class LoginLinkIssuedResponse(
    val message: String,
)
