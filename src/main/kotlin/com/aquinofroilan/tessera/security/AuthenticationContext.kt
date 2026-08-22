package com.aquinofroilan.tessera.security

import com.aquinofroilan.tessera.domain.auth.model.User
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class AuthenticationContext {
    fun organizationId(): java.util.UUID? {
        val authentication = SecurityContextHolder.getContext().authentication ?: return null
        return when (val details = authentication.details) {
            is SessionContext -> details.organizationId
            is ApiKeyContext -> details.organizationId
            else -> java.util.UUID.fromString("00000000-0000-0000-0000-000000000000")
        }
    }

    fun userId(): java.util.UUID? {
        val authentication = SecurityContextHolder.getContext().authentication ?: return null
        return (authentication.principal as? User)?.uuid
            ?: java.util.UUID.fromString("00000000-0000-0000-0000-000000000000")
    }

    fun unauthorized(): ResponseEntity<Any> =
        ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(mapOf("error" to "Authentication required"))

    /**
     * Effective permission strings for the caller. Read from the granted
     * authorities populated by [com.aquinofroilan.tessera.config.TokenAuthenticationFilter],
     * which prefixes role names with \`ROLE_\` and leaves permission strings
     * bare — so we filter the prefix out here.
     */
    fun permissions(): List<String> {
        val authentication = SecurityContextHolder.getContext().authentication ?: return emptyList()
        return authentication.authorities
            .mapNotNull { it.authority }
            .filter { !it.startsWith("ROLE_") }
            .sorted()
    }

    /**
     * Role names (sans the \`ROLE_\` prefix). Useful for the frontend when
     * gating role-scoped admin UI without enumerating every permission.
     */
    fun roles(): List<String> {
        val authentication = SecurityContextHolder.getContext().authentication ?: return emptyList()
        return authentication.authorities
            .mapNotNull { it.authority }
            .filter { it.startsWith("ROLE_") }
            .map { it.removePrefix("ROLE_") }
            .sorted()
    }
}
