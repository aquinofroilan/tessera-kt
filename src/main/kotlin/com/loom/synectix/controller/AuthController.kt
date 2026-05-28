package com.loom.synectix.controller

import com.github.benmanes.caffeine.cache.Caffeine
import com.loom.synectix.annotation.LogLevel
import com.loom.synectix.annotation.Loggable
import com.loom.synectix.dto.ChangePasswordRequest
import com.loom.synectix.dto.ForgotPasswordRequest
import com.loom.synectix.dto.LoginRequest
import com.loom.synectix.dto.RefreshRequest
import com.loom.synectix.dto.RegisterRequest
import com.loom.synectix.dto.ResetPasswordRequest
import com.loom.synectix.dto.SwitchOrganizationRequest
import com.loom.synectix.exception.AuthenticationException
import com.loom.synectix.model.User
import com.loom.synectix.security.SessionContext
import com.loom.synectix.service.AuthService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.Locale
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
        private const val MAX_USER_AGENT_LENGTH = 512
        private const val MAX_IP_LENGTH = 45
    }

    @PostMapping("/signup")
    fun register(
        @Valid @RequestBody request: RegisterRequest,
    ): ResponseEntity<Any> {
        val user = authService.register(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(mapOf("message" to "User registered successfully", "userId" to user.uuid))
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
            val response =
                authService.login(
                    request,
                    ipAddress = clientIp?.take(MAX_IP_LENGTH),
                    userAgent = httpRequest.getHeader("User-Agent")?.take(MAX_USER_AGENT_LENGTH),
                )
            resetAttempts(clientIp)
            ResponseEntity.ok(response)
        } catch (e: AuthenticationException) {
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
    ): ResponseEntity<Any> {
        val response = authService.refresh(request.refreshToken)
        return ResponseEntity.ok(response)
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

        authService.changePassword(user, request.currentPassword, request.newPassword)
        return ResponseEntity.ok(mapOf("message" to "Password changed successfully"))
    }

    @PostMapping("/forgot-password")
    fun forgotPassword(
        @Valid @RequestBody request: ForgotPasswordRequest,
    ): ResponseEntity<Any> {
        val email = request.email.lowercase(Locale.ROOT)
        if (forgotPasswordThrottle.getIfPresent(email) != null) {
            return ResponseEntity.ok(
                mapOf("message" to "If an account with that email exists, a password reset link has been sent."),
            )
        }

        forgotPasswordThrottle.put(email, true)
        try {
            authService.forgotPassword(email)
        } catch (e: Exception) {
            log.error("Forgot-password flow failed", e)
        }
        return ResponseEntity.ok(
            mapOf("message" to "If an account with that email exists, a password reset link has been sent."),
        )
    }

    @PostMapping("/reset-password")
    fun resetPassword(
        @Valid @RequestBody request: ResetPasswordRequest,
    ): ResponseEntity<Any> {
        authService.resetPassword(request.token, request.newPassword)
        return ResponseEntity.ok(mapOf("message" to "Password has been reset successfully"))
    }

    @GetMapping("/organizations")
    fun listOrganizations(): ResponseEntity<Any> {
        val authentication = SecurityContextHolder.getContext().authentication
        val user =
            authentication?.principal as? User
                ?: return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(mapOf("error" to "Authentication required"))
        val sessionContext =
            authentication.details as? SessionContext
                ?: return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(mapOf("error" to "Authentication required"))

        val orgs = authService.listUserOrganizations(user, sessionContext.organizationId)
        return ResponseEntity.ok(orgs)
    }

    @PostMapping("/organizations/switch")
    fun switchOrganization(
        @Valid @RequestBody request: SwitchOrganizationRequest,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<Any> {
        val authentication = SecurityContextHolder.getContext().authentication
        val user =
            authentication?.principal as? User
                ?: return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(mapOf("error" to "Authentication required"))

        val response =
            authService.switchOrganization(
                user = user,
                targetOrgId = request.organizationId,
                ipAddress = httpRequest.remoteAddr?.take(MAX_IP_LENGTH),
                userAgent = httpRequest.getHeader("User-Agent")?.take(MAX_USER_AGENT_LENGTH),
            )
        return ResponseEntity.ok(response)
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
        val newCount = loginAttemptCounts.asMap().merge(ip, 1, Int::plus) ?: 1
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
