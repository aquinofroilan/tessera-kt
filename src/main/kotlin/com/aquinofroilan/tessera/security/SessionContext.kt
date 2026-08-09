package com.aquinofroilan.tessera.security

data class SessionContext(
    val sessionId: java.util.UUID,
    val organizationId: java.util.UUID,
)
