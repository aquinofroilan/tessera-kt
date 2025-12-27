package com.froilan.synectix.service

import com.froilan.synectix.annotation.Loggable
import com.froilan.synectix.annotation.LogLevel
import com.froilan.synectix.dto.AuthResponse
import com.froilan.synectix.dto.LoginRequest
import com.froilan.synectix.dto.RegisterRequest
import com.froilan.synectix.model.SessionToken
import com.froilan.synectix.model.User
import com.froilan.synectix.repository.SessionTokenRepository
import com.froilan.synectix.repository.UserRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.time.LocalDateTime
import java.util.Base64


@Service
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class AuthService(
    private val userRepository: UserRepository,
    private val sessionTokenRepository: SessionTokenRepository,
    @Qualifier("argon2PasswordEncoder")
    private val passwordEncoder: PasswordEncoder,
) {

    private val secureRandom = SecureRandom()
    private val tokenValidityHours = 24L


    fun register(request: RegisterRequest): User {
        if (userRepository.existsByUsername(request.username)) {
            throw IllegalArgumentException("Username already exists")
        }
        if (userRepository.existsByEmail(request.email)) {
            throw IllegalArgumentException("Email already exists")
        }

        val user = User(
            username = request.username,
            passwordHash = passwordEncoder.encode(request.password) as String,
            email = request.email
        )
        return userRepository.save(user)
    }

    @Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
    fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByUsername(request.username)
            .orElseThrow { IllegalArgumentException("Invalid username or password") }

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw IllegalArgumentException("Invalid username or password")
        }

        val token = generateToken()
        val expiryAt = LocalDateTime.now().plusHours(tokenValidityHours)

        val sessionToken = SessionToken(
            token = token,
            userId = user.id,
            expiryAt = expiryAt
        )
        sessionTokenRepository.save(sessionToken)

        return AuthResponse(
            token = token,
            username = user.username,
            roles = user.roles,
            expiresAt = expiryAt.toString()
        )
    }

    fun logout(token: String) {
        sessionTokenRepository.deleteByToken(token)
    }

    private fun generateToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
