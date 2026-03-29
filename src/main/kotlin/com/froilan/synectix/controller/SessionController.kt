package com.froilan.synectix.controller

import com.froilan.synectix.dto.SessionResponse
import com.froilan.synectix.model.User
import com.froilan.synectix.service.AuthService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
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
) {
    @GetMapping
    fun listSessions(
        @RequestHeader("Authorization") authHeader: String,
    ): ResponseEntity<Any> {
        val (user, currentToken) =
            extractUserAndToken(authHeader)
                ?: return unauthorized()

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
    fun revokeSession(
        @PathVariable sessionId: String,
    ): ResponseEntity<Any> {
        val user = extractUser() ?: return unauthorized()

        return try {
            authService.revokeSession(user.uuid, sessionId)
            ResponseEntity.ok(mapOf("message" to "Session revoked"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to (e.message ?: "Session not found")))
        }
    }

    @DeleteMapping
    fun revokeOtherSessions(
        @RequestHeader("Authorization") authHeader: String,
    ): ResponseEntity<Any> {
        val (user, currentToken) =
            extractUserAndToken(authHeader)
                ?: return unauthorized()

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

    private fun unauthorized(): ResponseEntity<Any> =
        ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(mapOf("error" to "Authentication required"))
}
