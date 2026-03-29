package com.froilan.synectix.service

import com.froilan.synectix.dto.AuthResponse
import com.froilan.synectix.dto.LoginRequest
import com.froilan.synectix.dto.RegisterRequest
import com.froilan.synectix.model.Organizations
import com.froilan.synectix.model.RefreshToken
import com.froilan.synectix.model.SessionToken
import com.froilan.synectix.model.User
import com.froilan.synectix.repository.OrganizationRepository
import com.froilan.synectix.repository.RefreshTokenRepository
import com.froilan.synectix.repository.SessionTokenRepository
import com.froilan.synectix.repository.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Base64

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val organizationRepository: OrganizationRepository,
    private val sessionTokenRepository: SessionTokenRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val mongoTemplate: MongoTemplate,
    private val passwordEncoder: PasswordEncoder,
    @Value("\${security.jwt.expiration:86400000}")
    private val tokenValidityMs: Long,
    @Value("\${security.jwt.refresh-expiration:2592000000}")
    private val refreshTokenValidityMs: Long,
) {

    private val secureRandom = SecureRandom()

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
                organizationId = savedOrganization.uuid,
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

        if (!user.isActive) {
            throw IllegalArgumentException("User account is inactive")
        }

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw IllegalArgumentException("Invalid username or password")
        }

        val accessTokenStr = generateToken()
        val expiryAt = LocalDateTime.now().plus(tokenValidityMs, ChronoUnit.MILLIS)

        val sessionToken = SessionToken(
            token = accessTokenStr,
            expiryAt = expiryAt,
            userId = user.uuid,
        )
        val savedSession = sessionTokenRepository.save(sessionToken)

        val refreshTokenStr = generateToken()
        val refreshExpiryAt = LocalDateTime.now().plus(refreshTokenValidityMs, ChronoUnit.MILLIS)

        val refreshToken = RefreshToken(
            token = refreshTokenStr,
            userId = user.uuid,
            sessionTokenId = savedSession.id,
            expiryAt = refreshExpiryAt,
        )
        refreshTokenRepository.save(refreshToken)

        return AuthResponse(
            accessToken = accessTokenStr,
            refreshToken = refreshTokenStr,
            username = user.username,
            roles = user.roles,
            expiresAt = expiryAt.toString(),
            refreshTokenExpiresAt = refreshExpiryAt.toString(),
        )
    }

    @Transactional
    fun refresh(refreshToken: String): AuthResponse {
        // Validate the refresh token exists and is usable (non-atomic read for validation)
        val existing = refreshTokenRepository.findByToken(refreshToken)
            .orElseThrow { IllegalArgumentException("Invalid or expired refresh token") }

        if (!existing.expiryAt.isAfter(LocalDateTime.now())) {
            throw IllegalArgumentException("Invalid or expired refresh token")
        }

        val user = userRepository.findById(existing.userId)
            .orElseThrow { IllegalArgumentException("Invalid or expired refresh token") }

        if (!user.isActive) {
            throw IllegalArgumentException("User account is inactive")
        }

        // Save new token pair first to avoid lockout if writes fail on standalone MongoDB
        val accessTokenStr = generateToken()
        val expiryAt = LocalDateTime.now().plus(tokenValidityMs, ChronoUnit.MILLIS)

        val sessionToken = SessionToken(
            token = accessTokenStr,
            expiryAt = expiryAt,
            userId = user.uuid,
        )
        val savedSession = sessionTokenRepository.save(sessionToken)

        val refreshTokenStr = generateToken()
        val refreshExpiryAt = LocalDateTime.now().plus(refreshTokenValidityMs, ChronoUnit.MILLIS)

        val newRefreshToken = RefreshToken(
            token = refreshTokenStr,
            userId = user.uuid,
            sessionTokenId = savedSession.id,
            expiryAt = refreshExpiryAt,
        )
        refreshTokenRepository.save(newRefreshToken)

        // Atomically consume the old refresh token — prevents concurrent reuse.
        // If findAndRemove returns null, another request already consumed it; roll back.
        val consumed = mongoTemplate.findAndRemove(
            Query.query(Criteria.where("token").`is`(refreshToken)),
            RefreshToken::class.java,
        )
        if (consumed == null) {
            // Another concurrent request already consumed this token — clean up our new pair
            refreshTokenRepository.deleteByToken(refreshTokenStr)
            sessionTokenRepository.deleteById(savedSession.id)
            throw IllegalArgumentException("Invalid or expired refresh token")
        }

        // Delete old session token
        sessionTokenRepository.deleteById(existing.sessionTokenId)

        return AuthResponse(
            accessToken = accessTokenStr,
            refreshToken = refreshTokenStr,
            username = user.username,
            roles = user.roles,
            expiresAt = expiryAt.toString(),
            refreshTokenExpiresAt = refreshExpiryAt.toString(),
        )
    }

    @Transactional
    fun logout(token: String) {
        val sessionToken = sessionTokenRepository.findByToken(token)
        if (sessionToken.isPresent) {
            refreshTokenRepository.deleteBySessionTokenId(sessionToken.get().id)
            sessionTokenRepository.deleteByToken(token)
        }
    }

    private fun generateToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
