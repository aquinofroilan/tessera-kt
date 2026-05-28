package com.loom.synectix.security

import org.springframework.security.access.PermissionEvaluator
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import java.io.Serializable

@Component
class SynectixPermissionEvaluator : PermissionEvaluator {
    override fun hasPermission(
        authentication: Authentication,
        targetDomainObject: Any,
        permission: Any,
    ): Boolean {
        if (authentication.authorities.any { it.authority == "ROLE_SUPER_ADMIN" }) {
            return true
        }
        val permStr = permission as? String ?: return false
        return authentication.authorities.any { it.authority == permStr }
    }

    override fun hasPermission(
        authentication: Authentication,
        targetId: Serializable,
        targetType: String,
        permission: Any,
    ): Boolean = hasPermission(authentication, targetId as Any, permission)
}
