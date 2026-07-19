package com.aquinofroilan.tessera.security

data class ApiKeyPrincipal(
    val apiKeyId: java.util.UUID,
    val apiKeyName: String,
    val organizationId: java.util.UUID,
)
