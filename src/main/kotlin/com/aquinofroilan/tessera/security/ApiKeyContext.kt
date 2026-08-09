package com.aquinofroilan.tessera.security

data class ApiKeyContext(
    val apiKeyId: java.util.UUID,
    val organizationId: java.util.UUID,
)
