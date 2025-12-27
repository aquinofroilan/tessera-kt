package com.froilan.synectix.dto

data class LoginRequest(
    val username: String,
    val password: String
)

data class RegisterRequest(
    val username: String,
    val password: String,
    val email: String
)

data class AuthResponse(
    val token: String,
    val username: String,
    val roles: Set<String>,
    val expiresAt: String
)
