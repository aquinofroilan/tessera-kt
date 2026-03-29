package com.froilan.synectix.controller

import com.froilan.synectix.annotation.LogLevel
import com.froilan.synectix.annotation.Loggable
import com.froilan.synectix.dto.ChangePasswordRequest
import com.froilan.synectix.dto.ForgotPasswordRequest
import com.froilan.synectix.dto.LoginRequest
import com.froilan.synectix.dto.RefreshRequest
import com.froilan.synectix.dto.RegisterRequest
import com.froilan.synectix.dto.ResetPasswordRequest
import com.froilan.synectix.model.User
import com.froilan.synectix.service.AuthService
import com.github.benmanes.caffeine.cache.Caffeine
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.TimeUnit

@RestController
@RequestMapping("/auth")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class AuthController(
    private val authService: AuthService,
) {
    private val log = LoggerFactory.getLogger(AuthController::class.java)

    private val loginAttemptCounts =
        Caffeine
            .newBuilder()
            .expireAfterWrite(BLOCK_DURATION_MINUTES, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build<String, Int>()

    private val blockedIps =
        Caffeine
            .newBuilder()
            .expireAfterWrite(BLOCK_DURATION_MINUTES, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build<String, Boolean>()

    private val forgotPasswordThrottle =
        Caffeine
            .newBuilder()
            .expireAfterWrite(FORGOT_PASSWORD_THROTTLE_MINUTES, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build<String, Boolean>()

    companion object {
        private const val MAX_ATTEMPTS = 5
        private const val BLOCK_DURATION_MINUTES = 15L
        private const val FORGOT_PASSWORD_THROTTLE_MINUTES = 2L
    }

    @PostMapping("/signup")
    fun register(
        @Valid @RequestBody request: RegisterRequest,
    ): ResponseEntity<Any> =
        try {
            val user = authService.register(request)
            ResponseEntity.status(HttpStatus.CREATED).body(mapOf("message" to "User registered successfully", "userId" to user.uuid))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }

    @PostMapping("/signin")
    fun login(
        @Valid @RequestBody request: LoginRequest,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<Any> {
        val clientIp = httpRequest.remoteAddr

        if (isBlocked(clientIp)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
                mapOf(
                    "error" to "Too many login attempts. Please try again later.",
                ),
            )
        }

        return try {
            val response = authService.login(request)
            resetAttempts(clientIp)
            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            if (e.message != "User account is inactive") {
                recordFailedAttempt(clientIp)
            }
            ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("error" to (e.message ?: "Invalid username or password")))
        }
    }

    @PostMapping("/refresh")
    fun refresh(
        @Valid @RequestBody request: RefreshRequest,
    ): ResponseEntity<Any> =
        try {
            val response = authService.refresh(request.refreshToken)
            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("error" to (e.message ?: "Invalid or expired refresh token")))
        }

    @PostMapping("/change-password")
    fun changePassword(
        @Valid @RequestBody request: ChangePasswordRequest,
    ): ResponseEntity<Any> {
        val authentication = SecurityContextHolder.getContext().authentication
        val user =
            authentication?.principal as? User
                ?: return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(mapOf("error" to "Authentication required"))

        return try {
            authService.changePassword(user, request.currentPassword, request.newPassword)
            ResponseEntity.ok(mapOf("message" to "Password changed successfully"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Password change failed")))
        }
    }

    @PostMapping("/forgot-password")
    fun forgotPassword(
        @Valid @RequestBody request: ForgotPasswordRequest,
    ): ResponseEntity<Any> {
        val email = request.email.lowercase()
        if (forgotPasswordThrottle.getIfPresent(email) != null) {
            return ResponseEntity.ok(
                mapOf("message" to "If an account with that email exists, a password reset link has been sent."),
            )
        }

        forgotPasswordThrottle.put(email, true)
        authService.forgotPassword(email)
        log.info("Password reset flow completed for request")
        return ResponseEntity.ok(
            mapOf("message" to "If an account with that email exists, a password reset link has been sent."),
        )
    }

    @PostMapping("/reset-password")
    fun resetPassword(
        @Valid @RequestBody request: ResetPasswordRequest,
    ): ResponseEntity<Any> =
        try {
            authService.resetPassword(request.token, request.newPassword)
            ResponseEntity.ok(mapOf("message" to "Password has been reset successfully"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Password reset failed")))
        }

    @PostMapping("/logout")
    fun logout(
        @RequestHeader("Authorization") authHeader: String?,
    ): ResponseEntity<Any> {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            val token = authHeader.substring(7)
            authService.logout(token)
        }
        return ResponseEntity.ok(mapOf("message" to "Logged out successfully"))
    }

    private fun isBlocked(ip: String): Boolean = blockedIps.getIfPresent(ip) != null

    private fun recordFailedAttempt(ip: String) {
        val current = loginAttemptCounts.getIfPresent(ip) ?: 0
        val newCount = current + 1
        loginAttemptCounts.put(ip, newCount)

        if (newCount >= MAX_ATTEMPTS) {
            blockedIps.put(ip, true)
            loginAttemptCounts.invalidate(ip)
        }
    }

    private fun resetAttempts(ip: String) {
        loginAttemptCounts.invalidate(ip)
        blockedIps.invalidate(ip)
    }
}
