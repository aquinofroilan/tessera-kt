package com.froilan.synectix.service

import com.froilan.synectix.dto.AuthResponse
import com.froilan.synectix.dto.LoginRequest
import com.froilan.synectix.dto.RegisterRequest
import com.froilan.synectix.dto.UserOrganizationResponse
import com.froilan.synectix.model.Organizations
import com.froilan.synectix.model.PasswordResetToken
import com.froilan.synectix.model.RefreshToken
import com.froilan.synectix.model.RoleAssignment
import com.froilan.synectix.model.SessionToken
import com.froilan.synectix.model.User
import com.froilan.synectix.model.orgRoleNames
import com.froilan.synectix.model.systemRoleNames
import com.froilan.synectix.repository.OrganizationRepository
import com.froilan.synectix.repository.PasswordResetTokenRepository
import com.froilan.synectix.repository.RefreshTokenRepository
import com.froilan.synectix.repository.SessionTokenRepository
import com.froilan.synectix.repository.UserRepository
import com.froilan.synectix.util.TokenHasher
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Locale

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val organizationRepository: OrganizationRepository,
    private val sessionTokenRepository: SessionTokenRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordResetTokenRepository: PasswordResetTokenRepository,
    private val mongoTemplate: MongoTemplate,
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
    fun login(
        request: LoginRequest,
        ipAddress: String? = null,
        userAgent: String? = null,
    ): AuthResponse {
        val user =
            userRepository
                .findByUsername(request.username)
                .orElseThrow { IllegalArgumentException("Invalid username or password") }

        if (!user.isActive) {
            throw IllegalArgumentException("User account is inactive")
        }

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw IllegalArgumentException("Invalid username or password")
        }

        val accessTokenStr = generateToken()
        val expiryAt = LocalDateTime.now().plus(tokenValidityMs, ChronoUnit.MILLIS)

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
        val refreshExpiryAt = LocalDateTime.now().plus(refreshTokenValidityMs, ChronoUnit.MILLIS)

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
                .orElseThrow { IllegalArgumentException("Invalid or expired refresh token") }

        if (!existing.expiryAt.isAfter(LocalDateTime.now())) {
            throw IllegalArgumentException("Invalid or expired refresh token")
        }

        val user =
            userRepository
                .findById(existing.userId)
                .orElseThrow { IllegalArgumentException("Invalid or expired refresh token") }

        if (!user.isActive) {
            throw IllegalArgumentException("User account is inactive")
        }

        val oldSession = sessionTokenRepository.findById(existing.sessionTokenId).orElse(null)
        val orgId = oldSession?.organizationId ?: user.organizationId

        val accessTokenStr = generateToken()
        val expiryAt = LocalDateTime.now().plus(tokenValidityMs, ChronoUnit.MILLIS)

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
        val refreshExpiryAt = LocalDateTime.now().plus(refreshTokenValidityMs, ChronoUnit.MILLIS)

        val newRefreshToken =
            RefreshToken(
                tokenHash = newRefreshTokenHash,
                userId = user.uuid,
                sessionTokenId = savedSession.id,
                expiryAt = refreshExpiryAt,
            )
        refreshTokenRepository.save(newRefreshToken)

        val consumed =
            mongoTemplate.findAndRemove(
                Query.query(Criteria.where("tokenHash").`is`(refreshTokenHash)),
                RefreshToken::class.java,
            )
        if (consumed == null) {
            refreshTokenRepository.deleteByTokenHash(newRefreshTokenHash)
            sessionTokenRepository.deleteById(savedSession.id)
            throw IllegalArgumentException("Invalid or expired refresh token")
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
            throw IllegalArgumentException("Current password is incorrect")
        }
        if (currentPassword == newPassword) {
            throw IllegalArgumentException("New password must be different from current password")
        }
        val updatedUser = user.copy(passwordHash = passwordEncoder.encode(newPassword) as String)
        userRepository.save(updatedUser)

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
                expiryAt = LocalDateTime.now().plusMinutes(resetTokenExpiryMinutes),
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
                .orElseThrow { IllegalArgumentException("Invalid or expired reset token") }

        if (!existing.expiryAt.isAfter(LocalDateTime.now())) {
            passwordResetTokenRepository.deleteById(existing.id)
            throw IllegalArgumentException("Invalid or expired reset token")
        }

        val resetToken =
            mongoTemplate.findAndRemove(
                Query.query(Criteria.where("tokenHash").`is`(tokenHash)),
                PasswordResetToken::class.java,
            ) ?: throw IllegalArgumentException("Invalid or expired reset token")

        val user =
            userRepository
                .findById(resetToken.userId)
                .orElseThrow { IllegalArgumentException("Invalid or expired reset token") }

        val updatedUser = user.copy(passwordHash = passwordEncoder.encode(newPassword) as String)
        userRepository.save(updatedUser)

        passwordResetTokenRepository.deleteByUserId(user.uuid)
        sessionTokenRepository.deleteByUserId(user.uuid)
        refreshTokenRepository.deleteByUserId(user.uuid)
    }

    fun listSessions(userId: String): List<SessionToken> = sessionTokenRepository.findByUserIdAndExpiryAtAfter(userId, LocalDateTime.now())

    @Transactional
    fun revokeSession(
        userId: String,
        sessionId: String,
        currentToken: String,
    ) {
        val session =
            sessionTokenRepository.findById(sessionId).orElseThrow {
                IllegalArgumentException("Session not found")
            }
        if (session.userId != userId) {
            throw IllegalArgumentException("Session not found")
        }
        if (session.token == currentToken) {
            throw IllegalStateException("Cannot revoke the current session")
        }
        refreshTokenRepository.deleteBySessionTokenId(session.id)
        sessionTokenRepository.deleteById(session.id)
    }

    @Transactional
    fun revokeOtherSessions(
        userId: String,
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
        targetOrgId: String,
        ipAddress: String? = null,
        userAgent: String? = null,
    ): AuthResponse {
        val orgRoles = user.orgRoleNames(targetOrgId)
        if (orgRoles.isEmpty()) {
            throw IllegalArgumentException("You do not have access to this organization")
        }

        val org =
            organizationRepository.findById(targetOrgId).orElseThrow {
                IllegalArgumentException("Organization not found")
            }
        if (!org.isActive) {
            throw IllegalArgumentException("Organization is not active")
        }

        val accessTokenStr = generateToken()
        val expiryAt = LocalDateTime.now().plus(tokenValidityMs, ChronoUnit.MILLIS)

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
        val refreshExpiryAt = LocalDateTime.now().plus(refreshTokenValidityMs, ChronoUnit.MILLIS)

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
        currentOrgId: String,
    ): List<UserOrganizationResponse> {
        val orgIds =
            user.roleAssignments
                .mapNotNull { it.organizationId }
                .distinct()

        val orgsById =
            organizationRepository
                .findAllById(orgIds)
                .filter { it.isActive }
                .associateBy { it.uuid }

        return orgIds.mapNotNull { orgId ->
            val org = orgsById[orgId] ?: return@mapNotNull null
            UserOrganizationResponse(
                organizationId = orgId,
                name = org.name,
                orgSlug = org.orgSlug,
                roles = user.orgRoleNames(orgId),
                isCurrent = orgId == currentOrgId,
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

    private fun generateToken(): String = tokenHasher.generate()
}
