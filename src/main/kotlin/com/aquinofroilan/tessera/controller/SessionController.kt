package com.aquinofroilan.tessera.controller

import com.aquinofroilan.tessera.dto.SessionResponse
import com.aquinofroilan.tessera.model.User
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.service.AuthService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth/sessions")
class SessionController(
    private val authService: AuthService,
    private val authContext: AuthenticationContext,
) {
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('session:read')")
    fun listSessions(
        @RequestHeader("Authorization") authHeader: String,
    ): ResponseEntity<Any> {
        val (user, currentToken) =
            extractUserAndToken(authHeader)
                ?: return authContext.unauthorized()

        val sessions = authService.listSessions(user.uuid)
        val response =
            sessions.map { session ->
                SessionResponse(
                    id = session.id,
                    ipAddress = session.ipAddress,
                    userAgent = session.userAgent,
                    createdAt = session.createdAt.toString(),
                    expiresAt = session.expiryAt.toString(),
                    current = session.token == currentToken,
                )
            }
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{sessionId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('session:delete')")
    fun revokeSession(
        @RequestHeader("Authorization") authHeader: String,
        @PathVariable sessionId: java.util.UUID,
    ): ResponseEntity<Any> {
        val (user, currentToken) =
            extractUserAndToken(authHeader)
                ?: return authContext.unauthorized()

        authService.revokeSession(user.uuid, sessionId, currentToken)
        return ResponseEntity.ok(mapOf("message" to "Session revoked"))
    }

    @DeleteMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('session:delete')")
    fun revokeOtherSessions(
        @RequestHeader("Authorization") authHeader: String,
    ): ResponseEntity<Any> {
        val (user, currentToken) =
            extractUserAndToken(authHeader)
                ?: return authContext.unauthorized()

        authService.revokeOtherSessions(user.uuid, currentToken)
        return ResponseEntity.ok(mapOf("message" to "All other sessions revoked"))
    }

    private fun extractUser(): User? {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication?.principal as? User
    }

    private fun extractUserAndToken(authHeader: String): Pair<User, String>? {
        val user = extractUser() ?: return null
        if (!authHeader.startsWith("Bearer ")) return null
        return user to authHeader.substring(7)
    }
}
