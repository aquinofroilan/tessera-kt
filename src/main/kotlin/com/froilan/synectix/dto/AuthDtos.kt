package com.froilan.synectix.dto

import java.time.LocalDateTime

data class LoginRequest(
    val username: String,
    val password: String
)

data class RegisterRequest(
    val username: String,
    val password: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val orgName: String,
    val orgSlug: String,
    val orgDescription: String? = null,
    val orgBaseCurrency: String,
    val orgFiscalYearStart: LocalDateTime,
    val orgTimezone: String,
    val orgLegalName: String,
    val orgTradeName: String,
)

data class AuthResponse(
    val token: String,
    val username: String,
    val roles: List<String>,
    val expiresAt: String
)
