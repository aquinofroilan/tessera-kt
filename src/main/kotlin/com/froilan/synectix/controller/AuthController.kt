package com.froilan.synectix.controller

import com.froilan.synectix.annotation.Loggable
import com.froilan.synectix.annotation.LogLevel
import com.froilan.synectix.dto.LoginRequest
import com.froilan.synectix.dto.RegisterRequest
import com.froilan.synectix.service.AuthService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import java.util.concurrent.ConcurrentHashMap
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/auth")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class AuthController(
    private val authService: AuthService
) {

    // Simple in-memory rate limiting (IP -> (Attempts, BlockedUntil))
    private val loginAttempts = ConcurrentHashMap<String, Pair<Int, LocalDateTime>>()
    private val MAX_ATTEMPTS = 5
    private val BLOCK_DURATION_MINUTES = 15L

    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<Any> {
        return try {
            val user = authService.register(request)
            ResponseEntity.status(HttpStatus.CREATED).body(mapOf("message" to "User registered successfully", "userId" to user.id))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest, httpRequest: HttpServletRequest): ResponseEntity<Any> {
        val clientIp = httpRequest.remoteAddr

        if (isBlocked(clientIp)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(mapOf("error" to "Too many login attempts. Please try again later."))
        }

        return try {
            val response = authService.login(request)
            resetAttempts(clientIp)
            ResponseEntity.ok(response)
        } catch (_: IllegalArgumentException) {
            recordFailedAttempt(clientIp)
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "Invalid credentials"))
        }
    }

    @PostMapping("/logout")
    fun logout(@RequestHeader("Authorization") authHeader: String?): ResponseEntity<Any> {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            val token = authHeader.substring(7)
            authService.logout(token)
        }
        return ResponseEntity.ok(mapOf("message" to "Logged out successfully"))
    }

    private fun isBlocked(ip: String): Boolean {
        val (_, blockedUntil) = loginAttempts[ip] ?: return false
        if (LocalDateTime.now().isBefore(blockedUntil)) {
            return true
        }
        // Block expired
        if (LocalDateTime.now().isAfter(blockedUntil) && blockedUntil != LocalDateTime.MIN) {
             loginAttempts.remove(ip)
             return false
        }
        return false
    }

    private fun recordFailedAttempt(ip: String) {
        val (attempts, _) = loginAttempts.getOrDefault(ip, 0 to LocalDateTime.MIN)
        val newAttempts = attempts + 1
        
        if (newAttempts >= MAX_ATTEMPTS) {
            loginAttempts[ip] = newAttempts to LocalDateTime.now().plusMinutes(BLOCK_DURATION_MINUTES)
        } else {
            loginAttempts[ip] = newAttempts to LocalDateTime.MIN
        }
    }

    private fun resetAttempts(ip: String) {
        loginAttempts.remove(ip)
    }
}
