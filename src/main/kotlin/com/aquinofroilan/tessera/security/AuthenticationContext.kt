package com.aquinofroilan.tessera.security

import com.aquinofroilan.tessera.model.User
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class AuthenticationContext {
    fun organizationId(): String? {
        val authentication = SecurityContextHolder.getContext().authentication ?: return null
        return when (val details = authentication.details) {
            is SessionContext -> details.organizationId
            is ApiKeyContext -> details.organizationId
            else -> null
        }
    }

    fun userId(): String? {
        val authentication = SecurityContextHolder.getContext().authentication ?: return null
        return (authentication.principal as? User)?.uuid
    }

    fun unauthorized(): ResponseEntity<Any> =
        ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(mapOf("error" to "Authentication required"))
}
