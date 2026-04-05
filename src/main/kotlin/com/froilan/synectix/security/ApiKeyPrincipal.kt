package com.froilan.synectix.security

data class ApiKeyPrincipal(
    val apiKeyId: String,
    val apiKeyName: String,
    val organizationId: String,
)
