package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.AuthResponse
import com.aquinofroilan.tessera.dto.LoginRequest
import com.aquinofroilan.tessera.dto.RegisterRequest
import com.aquinofroilan.tessera.dto.UserOrganizationResponse
import com.aquinofroilan.tessera.exception.AuthenticationException
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.Organizations
import com.aquinofroilan.tessera.model.PasswordResetToken
import com.aquinofroilan.tessera.model.RefreshToken
import com.aquinofroilan.tessera.model.RoleAssignment
import com.aquinofroilan.tessera.model.SessionToken
import com.aquinofroilan.tessera.model.User
import com.aquinofroilan.tessera.model.orgRoleNames
import com.aquinofroilan.tessera.model.systemRoleNames
import com.aquinofroilan.tessera.repository.OrganizationRepository
import com.aquinofroilan.tessera.repository.PasswordResetTokenRepository
import com.aquinofroilan.tessera.repository.RefreshTokenRepository
import com.aquinofroilan.tessera.repository.SessionTokenRepository
import com.aquinofroilan.tessera.repository.UserRepository
import com.aquinofroilan.tessera.util.TokenHasher
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.Locale

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val organizationRepository: OrganizationRepository,
    private val sessionTokenRepository: SessionTokenRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordResetTokenRepository: PasswordResetTokenRepository,
    private val accountService: AccountService,
    private val jdbcTemplate: JdbcTemplate,
    private val tokenHasher: TokenHasher,
    private val passwordEncoder: PasswordEncoder,
    @Value("\${security.jwt.expiration:86400000}")
    private val tokenValidityMs: Long,
    @Value("\${security.jwt.refresh-expiration:2592000000}")
    private val refreshTokenValidityMs: Long,
    @Value("\${security.password-reset.expiration-minutes:60}")
    private val resetTokenExpiryMinutes: Long,
) {
    @Transactional
    fun register(request: RegisterRequest): User {
        try {
            val organization =
                Organizations(
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
            accountService.seedDefaultAccounts(savedOrganization.uuid)

            val user =
                User(
                    username = request.username,
                    passwordHash = passwordEncoder.encode(request.password) as String,
                    firstName = request.firstName,
                    lastName = request.lastName,
                    email = request.email.lowercase(Locale.ROOT),
                    organizationId = savedOrganization.uuid,
                    roleAssignments =
                        listOf(
                            RoleAssignment(role = "OWNER", organizationId = savedOrganization.uuid),
                        ),
                )
            return userRepository.save(user)
        } catch (e: DuplicateKeyException) {
            val errorMessage = e.message ?: ""
            when {
                errorMessage.contains("username", ignoreCase = true) ->
                    throw BusinessRuleException("Username already exists", e)
                errorMessage.contains("email", ignoreCase = true) ->
                    throw BusinessRuleException("Email already exists", e)
                errorMessage.contains("orgSlug", ignoreCase = true) ->
                    throw BusinessRuleException("Organization slug already exists", e)
                errorMessage.contains("name", ignoreCase = true) ->
                    throw BusinessRuleException("Organization name already exists", e)
                else ->
                    throw BusinessRuleException("Registration failed due to a conflict", e)
            }
        }
    }

    @Transactional
    fun login(
        request: LoginRequest,
        ipAddress: String? = null,
        userAgent: String? = null,
    ): AuthResponse {
        val user =
            userRepository
                .findByUsername(request.username)
                .orElseThrow { AuthenticationException("Invalid username or password") }

        if (!user.isActive) {
            throw AuthenticationException("User account is inactive")
        }

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw AuthenticationException("Invalid username or password")
        }

        val accessTokenStr = generateToken()
        val expiryAt = LocalDateTime.now(ZoneOffset.UTC).plus(tokenValidityMs, ChronoUnit.MILLIS)

        val orgId = user.organizationId
        val sessionToken =
            SessionToken(
                token = accessTokenStr,
                expiryAt = expiryAt,
                userId = user.uuid,
                organizationId = orgId,
                ipAddress = ipAddress,
                userAgent = userAgent,
            )
        val savedSession = sessionTokenRepository.save(sessionToken)

        val refreshTokenStr = generateToken()
        val refreshExpiryAt = LocalDateTime.now(ZoneOffset.UTC).plus(refreshTokenValidityMs, ChronoUnit.MILLIS)

        val refreshToken =
            RefreshToken(
                tokenHash = tokenHasher.hash(refreshTokenStr),
                userId = user.uuid,
                sessionTokenId = savedSession.id,
                expiryAt = refreshExpiryAt,
            )
        refreshTokenRepository.save(refreshToken)

        val roles = (user.orgRoleNames(orgId) + user.systemRoleNames()).distinct()
        return AuthResponse(
            accessToken = accessTokenStr,
            refreshToken = refreshTokenStr,
            username = user.username,
            roles = roles,
            organizationId = orgId,
            expiresAt = expiryAt.toString(),
            refreshTokenExpiresAt = refreshExpiryAt.toString(),
        )
    }

    @Transactional
    fun refresh(refreshToken: String): AuthResponse {
        val refreshTokenHash = tokenHasher.hash(refreshToken)
        val existing =
            refreshTokenRepository
                .findByTokenHash(refreshTokenHash)
                .orElseThrow { AuthenticationException("Invalid or expired refresh token") }

        if (!existing.expiryAt.isAfter(LocalDateTime.now(ZoneOffset.UTC))) {
            throw AuthenticationException("Invalid or expired refresh token")
        }

        val user =
            userRepository
                .findById(existing.userId)
                .orElseThrow { AuthenticationException("Invalid or expired refresh token") }

        if (!user.isActive) {
            throw AuthenticationException("User account is inactive")
        }

        val oldSession = sessionTokenRepository.findById(existing.sessionTokenId).orElse(null)
        val orgId = oldSession?.organizationId ?: user.organizationId

        val accessTokenStr = generateToken()
        val expiryAt = LocalDateTime.now(ZoneOffset.UTC).plus(tokenValidityMs, ChronoUnit.MILLIS)

        val sessionToken =
            SessionToken(
                token = accessTokenStr,
                expiryAt = expiryAt,
                userId = user.uuid,
                organizationId = orgId,
            )
        val savedSession = sessionTokenRepository.save(sessionToken)

        val refreshTokenStr = generateToken()
        val newRefreshTokenHash = tokenHasher.hash(refreshTokenStr)
        val refreshExpiryAt = LocalDateTime.now(ZoneOffset.UTC).plus(refreshTokenValidityMs, ChronoUnit.MILLIS)

        val newRefreshToken =
            RefreshToken(
                tokenHash = newRefreshTokenHash,
                userId = user.uuid,
                sessionTokenId = savedSession.id,
                expiryAt = refreshExpiryAt,
            )
        refreshTokenRepository.save(newRefreshToken)

        val consumed =
            jdbcTemplate.update(
                "DELETE FROM refresh_tokens WHERE token_hash = ?",
                refreshTokenHash,
            )
        if (consumed == 0) {
            refreshTokenRepository.deleteByTokenHash(newRefreshTokenHash)
            sessionTokenRepository.deleteById(savedSession.id)
            throw AuthenticationException("Invalid or expired refresh token")
        }

        sessionTokenRepository.deleteById(existing.sessionTokenId)

        val roles = (user.orgRoleNames(orgId) + user.systemRoleNames()).distinct()
        return AuthResponse(
            accessToken = accessTokenStr,
            refreshToken = refreshTokenStr,
            username = user.username,
            roles = roles,
            organizationId = orgId,
            expiresAt = expiryAt.toString(),
            refreshTokenExpiresAt = refreshExpiryAt.toString(),
        )
    }

    @Transactional
    fun changePassword(
        user: User,
        currentPassword: String,
        newPassword: String,
    ) {
        if (!passwordEncoder.matches(currentPassword, user.passwordHash)) {
            throw BusinessRuleException("Current password is incorrect")
        }
        if (currentPassword == newPassword) {
            throw BusinessRuleException("New password must be different from current password")
        }
        user.passwordHash = passwordEncoder.encode(newPassword) as String
        userRepository.save(user)

        sessionTokenRepository.deleteByUserId(user.uuid)
        refreshTokenRepository.deleteByUserId(user.uuid)
    }

    @Transactional
    fun forgotPassword(email: String): String? {
        val normalizedEmail = email.lowercase(Locale.ROOT)
        val user = userRepository.findByEmail(normalizedEmail).orElse(null) ?: return null

        if (!user.isActive) {
            return null
        }

        passwordResetTokenRepository.deleteByUserId(user.uuid)

        val rawToken = generateToken()
        val resetToken =
            PasswordResetToken(
                tokenHash = tokenHasher.hash(rawToken),
                userId = user.uuid,
                expiryAt = LocalDateTime.now(ZoneOffset.UTC).plusMinutes(resetTokenExpiryMinutes),
            )
        passwordResetTokenRepository.save(resetToken)

        return rawToken
    }

    @Transactional
    fun resetPassword(
        token: String,
        newPassword: String,
    ) {
        val tokenHash = tokenHasher.hash(token)

        val existing =
            passwordResetTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow { BusinessRuleException("Invalid or expired reset token") }

        if (!existing.expiryAt.isAfter(LocalDateTime.now(ZoneOffset.UTC))) {
            passwordResetTokenRepository.deleteById(existing.id)
            throw BusinessRuleException("Invalid or expired reset token")
        }

        val deleted =
            jdbcTemplate.update(
                "DELETE FROM password_reset_tokens WHERE token_hash = ?",
                tokenHash,
            )
        if (deleted == 0) {
            throw BusinessRuleException("Invalid or expired reset token")
        }

        val user =
            userRepository
                .findById(existing.userId)
                .orElseThrow { BusinessRuleException("Invalid or expired reset token") }

        user.passwordHash = passwordEncoder.encode(newPassword) as String
        userRepository.save(user)

        passwordResetTokenRepository.deleteByUserId(user.uuid)
        sessionTokenRepository.deleteByUserId(user.uuid)
        refreshTokenRepository.deleteByUserId(user.uuid)
    }

    fun listSessions(userId: java.util.UUID): List<SessionToken> =
        sessionTokenRepository.findByUserIdAndExpiryAtAfter(userId, LocalDateTime.now(ZoneOffset.UTC))

    @Transactional
    fun revokeSession(
        userId: java.util.UUID,
        sessionId: java.util.UUID,
        currentToken: String,
    ) {
        val session =
            sessionTokenRepository.findById(sessionId).orElseThrow {
                ResourceNotFoundException("Session not found")
            }
        if (session.userId != userId) {
            throw ResourceNotFoundException("Session not found")
        }
        if (session.token == currentToken) {
            throw BusinessRuleException("Cannot revoke the current session")
        }
        refreshTokenRepository.deleteBySessionTokenId(session.id)
        sessionTokenRepository.deleteById(session.id)
    }

    @Transactional
    fun revokeOtherSessions(
        userId: java.util.UUID,
        currentToken: String,
    ) {
        val otherSessions = sessionTokenRepository.findByUserIdAndTokenNot(userId, currentToken)
        if (otherSessions.isEmpty()) return

        val sessionIds = otherSessions.map { it.id }
        refreshTokenRepository.deleteBySessionTokenIdIn(sessionIds)
        sessionTokenRepository.deleteAllById(sessionIds)
    }

    @Transactional
    fun switchOrganization(
        user: User,
        targetOrgId: java.util.UUID,
        ipAddress: String? = null,
        userAgent: String? = null,
    ): AuthResponse {
        val orgRoles = user.orgRoleNames(targetOrgId)
        if (orgRoles.isEmpty()) {
            throw BusinessRuleException("You do not have access to this organization")
        }

        val org =
            organizationRepository.findById(targetOrgId).orElseThrow {
                ResourceNotFoundException("Organization not found")
            }
        if (!org.isActive) {
            throw BusinessRuleException("Organization is not active")
        }

        val accessTokenStr = generateToken()
        val expiryAt = LocalDateTime.now(ZoneOffset.UTC).plus(tokenValidityMs, ChronoUnit.MILLIS)

        val sessionToken =
            SessionToken(
                token = accessTokenStr,
                expiryAt = expiryAt,
                userId = user.uuid,
                organizationId = targetOrgId,
                ipAddress = ipAddress,
                userAgent = userAgent,
            )
        val savedSession = sessionTokenRepository.save(sessionToken)

        val refreshTokenStr = generateToken()
        val refreshExpiryAt = LocalDateTime.now(ZoneOffset.UTC).plus(refreshTokenValidityMs, ChronoUnit.MILLIS)

        val refreshToken =
            RefreshToken(
                tokenHash = tokenHasher.hash(refreshTokenStr),
                userId = user.uuid,
                sessionTokenId = savedSession.id,
                expiryAt = refreshExpiryAt,
            )
        refreshTokenRepository.save(refreshToken)

        val roles = (orgRoles + user.systemRoleNames()).distinct()
        return AuthResponse(
            accessToken = accessTokenStr,
            refreshToken = refreshTokenStr,
            username = user.username,
            roles = roles,
            organizationId = targetOrgId,
            expiresAt = expiryAt.toString(),
            refreshTokenExpiresAt = refreshExpiryAt.toString(),
        )
    }

    fun listUserOrganizations(
        user: User,
        currentOrgId: java.util.UUID,
    ): List<UserOrganizationResponse> {
        val orgIds =
            user.roleAssignments
                .mapNotNull { it.organizationId }
                .distinct()

        val orgsById =
            organizationRepository
                .findAllById(orgIds)
                .associateBy { it.uuid }

        return orgIds.map { orgId ->
            val org = orgsById[orgId]
            UserOrganizationResponse(
                organizationId = orgId,
                name = org?.name ?: "Unknown",
                orgSlug = org?.orgSlug ?: "unknown",
                roles = user.orgRoleNames(orgId).distinct(),
                isCurrent = orgId == currentOrgId,
                isActive = org?.isActive ?: false,
            )
        }
    }

    @Transactional
    fun logout(token: String) {
        val sessionToken = sessionTokenRepository.findByToken(token)
        if (sessionToken.isPresent) {
            refreshTokenRepository.deleteBySessionTokenId(sessionToken.get().id)
            sessionTokenRepository.deleteByToken(token)
        }
    }

    private fun generateToken(): String = tokenHasher.generate(32)
}
