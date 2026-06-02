package com.aquinofroilan.tessera.dto

/**
 * Returned by GET /auth/me/permissions. Frontend uses this to gate admin
 * surfaces (nav entries, route guards) without enumerating every
 * permission string in JS — the source of truth lives on the server.
 */
data class CallerPermissionsResponse(
    val userId: String,
    val organizationId: String?,
    val roles: List<String>,
    val permissions: List<String>,
)
