package com.aquinofroilan.tessera.security

data class SessionContext(
    val sessionId: String,
    val organizationId: String,
)
