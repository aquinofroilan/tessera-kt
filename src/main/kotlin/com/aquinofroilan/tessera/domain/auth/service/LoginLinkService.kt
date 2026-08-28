package com.aquinofroilan.tessera.domain.auth.service

import com.aquinofroilan.tessera.domain.auth.dto.AuthResponse
import com.aquinofroilan.tessera.domain.auth.model.LoginLinkToken
import com.aquinofroilan.tessera.domain.auth.model.RefreshToken
import com.aquinofroilan.tessera.domain.auth.model.SessionToken
import com.aquinofroilan.tessera.domain.auth.model.orgRoleNames
import com.aquinofroilan.tessera.domain.auth.model.systemRoleNames
import com.aquinofroilan.tessera.domain.auth.repository.LoginLinkTokenRepository
import com.aquinofroilan.tessera.domain.auth.repository.RefreshTokenRepository
import com.aquinofroilan.tessera.domain.auth.repository.SessionTokenRepository
import com.aquinofroilan.tessera.domain.auth.repository.UserRepository
import com.aquinofroilan.tessera.exception.AuthenticationException
import com.aquinofroilan.tessera.util.TokenHasher
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Passwordless login via single-use, short-lived magic-link tokens.
 *
 * Flow:
 * 1. `request(email)` -> if the account exists and is active, generate a raw
 *    token, store its SHA-256 hash, and return the raw token to the caller
 *    (the controller is expected to deliver it via email and **not** echo it
 *    back to the client). Returns null for unknown / inactive emails so the
 *    public response can be uniform and unenumerable.
 * 2. `consume(rawToken, ip, ua)` -> looks the hash up, requires unconsumed +
 *    unexpired, marks consumed, mints a session + refresh token and returns
 *    the same AuthResponse shape as password login. Single-use: a second
 *    consume of the same token fails.
 *
 * Outstanding magic-link tokens are wiped when a new one is requested, so
 * only the most recent link mailed to a user is usable.
 */
@Service
class LoginLinkService(
    private val userRepository: UserRepository,
    private val loginLinkTokenRepository: LoginLinkTokenRepository,
    private val sessionTokenRepository: SessionTokenRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val tokenHasher: TokenHasher,
    @Value("\${security.login-link.expiration-minutes:15}")
    private val linkExpiryMinutes: Long,
    @Value("\${security.jwt.expiration:86400000}")
    private val tokenValidityMs: Long,
    @Value("\${security.jwt.refresh-expiration:2592000000}")
    private val refreshTokenValidityMs: Long,
) {
    @Transactional
    fun request(email: String): String? {
        val normalised = email.lowercase(Locale.ROOT)
        val user = userRepository.findByEmail(normalised).orElse(null) ?: return null
        if (!user.isActive) return null

        loginLinkTokenRepository.deleteByUserId(user.uuid)

        val rawToken = tokenHasher.generate(32)
        val record =
            LoginLinkToken(
                tokenHash = tokenHasher.hash(rawToken),
                userId = user.uuid,
                expiryAt = LocalDateTime.now(ZoneOffset.UTC).plusMinutes(linkExpiryMinutes),
            )
        loginLinkTokenRepository.save(record)
        return rawToken
    }

    @Transactional
    fun consume(
        rawToken: String,
        ipAddress: String? = null,
        userAgent: String? = null,
    ): AuthResponse {
        val tokenHash = tokenHasher.hash(rawToken)
        val record =
            loginLinkTokenRepository.findByTokenHash(tokenHash).orElseThrow {
                AuthenticationException("Invalid or expired login link")
            }
        val now = LocalDateTime.now(ZoneOffset.UTC)
        if (record.consumedAt != null) {
            throw AuthenticationException("Invalid or expired login link")
        }
        if (!record.expiryAt.isAfter(now)) {
            loginLinkTokenRepository.deleteById(record.id)
            throw AuthenticationException("Invalid or expired login link")
        }

        val user =
            userRepository.findById(record.userId).orElseThrow {
                AuthenticationException("Invalid or expired login link")
            }
        if (!user.isActive) {
            throw AuthenticationException("User account is inactive")
        }

        record.consumedAt = now
        record.ipAddress = ipAddress
        record.userAgent = userAgent
        loginLinkTokenRepository.save(record)

        val accessTokenStr = tokenHasher.generate(32)
        val accessExpiryAt = LocalDateTime.now(ZoneOffset.UTC).plus(tokenValidityMs, ChronoUnit.MILLIS)
        val orgId = user.organizationId
        val savedSession =
            sessionTokenRepository.save(
                SessionToken(
                    token = accessTokenStr,
                    expiryAt = accessExpiryAt,
                    userId = user.uuid,
                    organizationId = orgId,
                    ipAddress = ipAddress,
                    userAgent = userAgent,
                ),
            )

        val refreshTokenStr = tokenHasher.generate(32)
        val refreshExpiryAt = LocalDateTime.now(ZoneOffset.UTC).plus(refreshTokenValidityMs, ChronoUnit.MILLIS)
        refreshTokenRepository.save(
            RefreshToken(
                tokenHash = tokenHasher.hash(refreshTokenStr),
                userId = user.uuid,
                sessionTokenId = savedSession.id,
                expiryAt = refreshExpiryAt,
            ),
        )

        val roles = (user.orgRoleNames(orgId) + user.systemRoleNames()).distinct()
        return AuthResponse(
            accessToken = accessTokenStr,
            refreshToken = refreshTokenStr,
            username = user.username,
            roles = roles,
            organizationId = orgId,
            expiresAt = accessExpiryAt.toString(),
            refreshTokenExpiresAt = refreshExpiryAt.toString(),
        )
    }
}
