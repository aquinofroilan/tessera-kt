package com.froilan.synectix.service

import com.froilan.synectix.dto.AuthResponse
import com.froilan.synectix.dto.LoginRequest
import com.froilan.synectix.dto.RegisterRequest
import com.froilan.synectix.model.Organizations
import com.froilan.synectix.model.SessionToken
import com.froilan.synectix.model.User
import com.froilan.synectix.repository.OrganizationRepository
import com.froilan.synectix.repository.SessionTokenRepository
import com.froilan.synectix.repository.UserRepository
import org.springframework.dao.DuplicateKeyException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.LocalDateTime
import java.util.*


@Service
class AuthService(
    private val userRepository: UserRepository,
    private val organizationRepository: OrganizationRepository,
    private val sessionTokenRepository: SessionTokenRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    private val secureRandom = SecureRandom()
    private val tokenValidityHours = 24L


    @Transactional
    fun register(request: RegisterRequest): User {
        try {
            val organization = Organizations(
                name = request.orgName,
                orgSlug = request.orgSlug,
                description = request.orgDescription,
                baseCurrency = request.orgBaseCurrency,
                fiscalYearStart = request.orgFiscalYearStart,
                tradeName = request.orgTradeName,
                timezone = request.orgTimezone,
                legalName = request.orgLegalName,
            )
            val savedOrganization = organizationRepository.save(organization)

            val user = User(
                username = request.username,
                passwordHash = passwordEncoder.encode(request.password) as String,
                firstName = request.firstName,
                lastName = request.lastName,
                email = request.email,
                organizationId = savedOrganization.uuid
            )
            return userRepository.save(user)

        } catch (e: DuplicateKeyException) {
            val errorMessage = e.message ?: ""
            when {
                errorMessage.contains("username", ignoreCase = true) ->
                    throw IllegalArgumentException("Username already exists")
                errorMessage.contains("email", ignoreCase = true) ->
                    throw IllegalArgumentException("Email already exists")
                errorMessage.contains("orgSlug", ignoreCase = true) ->
                    throw IllegalArgumentException("Organization slug already exists")
                errorMessage.contains("name", ignoreCase = true) ->
                    throw IllegalArgumentException("Organization name already exists")
                else ->
                    throw IllegalArgumentException("Duplicate entry: ${e.message}")
            }
        }
    }

    @Transactional
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
            expiryAt = expiryAt,
            userId = user.uuid
        )
        sessionTokenRepository.save(sessionToken)

        return AuthResponse(
            token = token,
            username = user.username,
            roles = user.roles,
            expiresAt = expiryAt.toString()
        )
    }

    @Transactional
    fun logout(token: String) {
        sessionTokenRepository.deleteByToken(token)
    }

    private fun generateToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
