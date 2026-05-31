package com.aquinofroilan.tessera.security

data class ApiKeyPrincipal(
    val apiKeyId: String,
    val apiKeyName: String,
    val organizationId: String,
)
