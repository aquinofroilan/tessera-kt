package com.aquinofroilan.tessera.security

data class ApiKeyContext(
    val apiKeyId: String,
    val organizationId: String,
)
