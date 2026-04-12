package com.froilan.synectix.security

import com.froilan.synectix.model.User
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
}
