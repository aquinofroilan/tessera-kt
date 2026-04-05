package com.froilan.synectix.service

import com.froilan.synectix.dto.LoginRequest
import com.froilan.synectix.dto.RegisterRequest
import com.froilan.synectix.model.Organizations
import com.froilan.synectix.model.PasswordResetToken
import com.froilan.synectix.model.RefreshToken
import com.froilan.synectix.model.RoleAssignment
import com.froilan.synectix.model.SessionToken
import com.froilan.synectix.model.User
import com.froilan.synectix.model.orgRoleNames
import com.froilan.synectix.repository.OrganizationRepository
import com.froilan.synectix.repository.PasswordResetTokenRepository
import com.froilan.synectix.repository.RefreshTokenRepository
import com.froilan.synectix.repository.SessionTokenRepository
import com.froilan.synectix.repository.UserRepository
import com.froilan.synectix.util.TokenHasher
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

class AuthServiceTest {
    private lateinit var authService: AuthService
    private lateinit var userRepository: UserRepository
    private lateinit var organizationRepository: OrganizationRepository
    private lateinit var sessionTokenRepository: SessionTokenRepository
    private lateinit var refreshTokenRepository: RefreshTokenRepository
    private lateinit var passwordResetTokenRepository: PasswordResetTokenRepository
    private lateinit var accountService: AccountService
    private lateinit var mongoTemplate: MongoTemplate
    private lateinit var tokenHasher: TokenHasher
    private lateinit var passwordEncoder: PasswordEncoder

    @BeforeEach
    fun setup() {
        userRepository = mock(UserRepository::class.java)
        organizationRepository = mock(OrganizationRepository::class.java)
        sessionTokenRepository = mock(SessionTokenRepository::class.java)
        refreshTokenRepository = mock(RefreshTokenRepository::class.java)
        passwordResetTokenRepository = mock(PasswordResetTokenRepository::class.java)
        accountService = mock(AccountService::class.java)
        mongoTemplate = mock(MongoTemplate::class.java)
        tokenHasher = mock(TokenHasher::class.java)
        passwordEncoder = mock(PasswordEncoder::class.java)

        `when`(tokenHasher.hash(any())).thenAnswer { "hashed-${it.arguments[0]}" }
        `when`(tokenHasher.generate(any())).thenAnswer { UUID.randomUUID().toString() }

        authService =
            AuthService(
                userRepository = userRepository,
                organizationRepository = organizationRepository,
                sessionTokenRepository = sessionTokenRepository,
                refreshTokenRepository = refreshTokenRepository,
                passwordResetTokenRepository = passwordResetTokenRepository,
                accountService = accountService,
                mongoTemplate = mongoTemplate,
                tokenHasher = tokenHasher,
                passwordEncoder = passwordEncoder,
                tokenValidityMs = 86400000L,
                refreshTokenValidityMs = 2592000000L,
                resetTokenExpiryMinutes = 60L,
            )
    }

    @Test
    fun `register should create organization and user successfully`() {
        val request = createValidRegisterRequest()
        val encodedPassword = "encodedPassword123"
        val savedOrg =
            Organizations(
                uuid = "org-123",
                name = request.orgName,
                orgSlug = request.orgSlug,
                description = request.orgDescription,
                baseCurrency = request.orgBaseCurrency,
                fiscalYearStart = request.orgFiscalYearStart,
                timezone = request.orgTimezone,
                legalName = request.orgLegalName,
                tradeName = request.orgTradeName,
            )
        val savedUser =
            User(
                uuid = "user-123",
                username = request.username,
                email = request.email,
                firstName = request.firstName,
                lastName = request.lastName,
                passwordHash = encodedPassword,
                organizationId = savedOrg.uuid,
            )

        `when`(passwordEncoder.encode(request.password)).thenReturn(encodedPassword)
        `when`(organizationRepository.save(any<Organizations>())).thenReturn(savedOrg)
        `when`(userRepository.save(any<User>())).thenReturn(savedUser)

        val result = authService.register(request)

        assertThat(result).isNotNull()
        assertThat(result.username).isEqualTo(request.username)
        assertThat(result.email).isEqualTo(request.email)
        assertThat(result.organizationId).isEqualTo(savedOrg.uuid)

        verify(organizationRepository, times(1)).save(any<Organizations>())

        val userCaptor = argumentCaptor<User>()
        verify(userRepository, times(1)).save(userCaptor.capture())
        assertThat(userCaptor.firstValue.passwordHash).isEqualTo(encodedPassword)
        assertThat(userCaptor.firstValue.organizationId).isEqualTo(savedOrg.uuid)
        assertThat(userCaptor.firstValue.roleAssignments).isEqualTo(listOf(RoleAssignment("OWNER", savedOrg.uuid)))
        verify(accountService).seedDefaultAccounts(savedOrg.uuid)
    }

    @Test
    fun `register should throw exception when username already exists`() {
        val request = createValidRegisterRequest()
        `when`(passwordEncoder.encode(any())).thenReturn("encodedPassword")
        `when`(organizationRepository.save(any<Organizations>()))
            .thenReturn(createMockOrganization())
        `when`(userRepository.save(any<User>()))
            .thenThrow(DuplicateKeyException("E11000 duplicate key error collection: synectix.users index: username"))

        val exception =
            assertThrows<IllegalArgumentException> {
                authService.register(request)
            }
        assertThat(exception.message).isEqualTo("Username already exists")
    }

    @Test
    fun `register should throw exception when email already exists`() {
        val request = createValidRegisterRequest()
        `when`(passwordEncoder.encode(any())).thenReturn("encodedPassword")
        `when`(organizationRepository.save(any<Organizations>()))
            .thenReturn(createMockOrganization())
        `when`(userRepository.save(any<User>()))
            .thenThrow(DuplicateKeyException("E11000 duplicate key error collection: synectix.users index: email"))

        val exception =
            assertThrows<IllegalArgumentException> {
                authService.register(request)
            }
        assertThat(exception.message).isEqualTo("Email already exists")
    }

    @Test
    fun `register should throw exception when organization slug already exists`() {
        val request = createValidRegisterRequest()
        `when`(organizationRepository.save(any<Organizations>()))
            .thenThrow(DuplicateKeyException("E11000 duplicate key error collection: synectix.organizations index: orgSlug"))

        val exception =
            assertThrows<IllegalArgumentException> {
                authService.register(request)
            }
        assertThat(exception.message).isEqualTo("Organization slug already exists")
        verify(accountService, never()).seedDefaultAccounts(any())
    }

    @Test
    fun `register should throw exception when organization name already exists`() {
        val request = createValidRegisterRequest()
        `when`(organizationRepository.save(any<Organizations>()))
            .thenThrow(DuplicateKeyException("E11000 duplicate key error collection: synectix.organizations index: name"))

        val exception =
            assertThrows<IllegalArgumentException> {
                authService.register(request)
            }
        assertThat(exception.message).isEqualTo("Organization name already exists")
    }

    @Test
    fun `register should encode password before saving`() {
        val request = createValidRegisterRequest()
        val encodedPassword = "super-secure-encoded-password"

        `when`(passwordEncoder.encode(request.password)).thenReturn(encodedPassword)
        `when`(organizationRepository.save(any<Organizations>())).thenReturn(createMockOrganization())
        `when`(userRepository.save(any<User>())).thenReturn(createMockUser())

        authService.register(request)

        verify(passwordEncoder, times(1)).encode(request.password)
        val userCaptor = argumentCaptor<User>()
        verify(userRepository).save(userCaptor.capture())
        assertThat(userCaptor.firstValue.passwordHash).isEqualTo(encodedPassword)
    }

    @Test
    fun `login should return auth response with valid credentials`() {
        val request = LoginRequest(username = "testuser", password = "password123")
        val user =
            User(
                uuid = "user-123",
                username = request.username,
                email = "test@example.com",
                firstName = "Test",
                lastName = "User",
                passwordHash = "encodedPassword",
                organizationId = "org-123",
                roleAssignments =
                    listOf(
                        RoleAssignment("MEMBER", "org-123"),
                        RoleAssignment("ADMIN", "org-123"),
                    ),
            )
        val savedToken = createMockSessionToken()

        `when`(userRepository.findByUsername(request.username)).thenReturn(Optional.of(user))
        `when`(passwordEncoder.matches(request.password, user.passwordHash)).thenReturn(true)
        `when`(sessionTokenRepository.save(any<SessionToken>())).thenReturn(savedToken)
        `when`(refreshTokenRepository.save(any<RefreshToken>())).thenAnswer { it.arguments[0] }

        val result = authService.login(request)

        assertThat(result).isNotNull()
        assertThat(result.username).isEqualTo(user.username)
        assertThat(result.roles).isEqualTo(user.orgRoleNames(user.organizationId))
        assertThat(result.organizationId).isEqualTo(user.organizationId)
        assertThat(result.accessToken).isNotNull()
        assertThat(result.expiresAt).isNotNull()

        verify(sessionTokenRepository, times(1)).save(any<SessionToken>())
    }

    @Test
    fun `login should return auth response with access token and refresh token`() {
        val request = LoginRequest(username = "testuser", password = "password123")
        val user = createMockUser()
        val savedToken = createMockSessionToken()

        `when`(userRepository.findByUsername(request.username)).thenReturn(Optional.of(user))
        `when`(passwordEncoder.matches(request.password, user.passwordHash)).thenReturn(true)
        `when`(sessionTokenRepository.save(any<SessionToken>())).thenReturn(savedToken)
        `when`(refreshTokenRepository.save(any<RefreshToken>())).thenAnswer { it.arguments[0] }

        val result = authService.login(request)

        assertThat(result.accessToken).isNotNull().isNotEmpty()
        assertThat(result.refreshToken).isNotNull().isNotEmpty()

        val expectedRefreshExpiry = LocalDateTime.now().plusDays(30)
        val actualRefreshExpiry = LocalDateTime.parse(result.refreshTokenExpiresAt)
        assertThat(actualRefreshExpiry).isAfter(expectedRefreshExpiry.minusMinutes(1))
        assertThat(actualRefreshExpiry).isBefore(expectedRefreshExpiry.plusMinutes(1))
    }

    @Test
    fun `login should throw exception with invalid username`() {
        val request = LoginRequest(username = "nonexistent", password = "password123")
        `when`(userRepository.findByUsername(request.username)).thenReturn(Optional.empty())

        val exception =
            assertThrows<IllegalArgumentException> {
                authService.login(request)
            }
        assertThat(exception.message).isEqualTo("Invalid username or password")
    }

    @Test
    fun `login should throw exception with invalid password`() {
        val request = LoginRequest(username = "testuser", password = "wrongpassword")
        val user = createMockUser()

        `when`(userRepository.findByUsername(request.username)).thenReturn(Optional.of(user))
        `when`(passwordEncoder.matches(request.password, user.passwordHash)).thenReturn(false)

        val exception =
            assertThrows<IllegalArgumentException> {
                authService.login(request)
            }
        assertThat(exception.message).isEqualTo("Invalid username or password")
    }

    @Test
    fun `login should create session token with 24 hour expiry`() {
        val request = LoginRequest(username = "testuser", password = "password123")
        val user = createMockUser()

        `when`(userRepository.findByUsername(request.username)).thenReturn(Optional.of(user))
        `when`(passwordEncoder.matches(request.password, user.passwordHash)).thenReturn(true)
        `when`(sessionTokenRepository.save(any<SessionToken>())).thenReturn(createMockSessionToken())
        `when`(refreshTokenRepository.save(any<RefreshToken>())).thenAnswer { it.arguments[0] }

        authService.login(request)

        val tokenCaptor = argumentCaptor<SessionToken>()
        verify(sessionTokenRepository).save(tokenCaptor.capture())

        val capturedToken = tokenCaptor.firstValue
        assertThat(capturedToken.userId).isEqualTo(user.uuid)

        val expectedExpiry = LocalDateTime.now().plusHours(24)
        val actualExpiry = capturedToken.expiryAt
        assertThat(actualExpiry).isAfter(expectedExpiry.minusMinutes(1))
        assertThat(actualExpiry).isBefore(expectedExpiry.plusMinutes(1))
    }

    @Test
    fun `login should generate secure random token`() {
        val request = LoginRequest(username = "testuser", password = "password123")
        val user = createMockUser()

        `when`(userRepository.findByUsername(request.username)).thenReturn(Optional.of(user))
        `when`(passwordEncoder.matches(request.password, user.passwordHash)).thenReturn(true)
        `when`(sessionTokenRepository.save(any<SessionToken>())).thenAnswer { it.arguments[0] }
        `when`(refreshTokenRepository.save(any<RefreshToken>())).thenAnswer { it.arguments[0] }

        val result1 = authService.login(request)
        val result2 = authService.login(request)

        assertThat(result2.accessToken).isNotEqualTo(result1.accessToken)
        assertThat(result1.accessToken).matches("^[A-Za-z0-9_-]+$")
    }

    @Test
    fun `login should throw exception when user account is inactive`() {
        val request = LoginRequest(username = "testuser", password = "password123")
        val inactiveUser = createMockUser().copy(isActive = false)

        `when`(userRepository.findByUsername(request.username)).thenReturn(Optional.of(inactiveUser))

        val exception =
            assertThrows<IllegalArgumentException> {
                authService.login(request)
            }
        assertThat(exception.message).isEqualTo("User account is inactive")
    }

    @Test
    fun `refresh should return new token pair with valid refresh token`() {
        val oldRefreshTokenStr = "old-refresh-token"
        val oldRefreshTokenHash = "hashed-$oldRefreshTokenStr"
        val user = createMockUser()
        val oldSessionToken = createMockSessionToken()
        val existingRefreshToken =
            RefreshToken(
                id = "rt-123",
                tokenHash = oldRefreshTokenHash,
                userId = user.uuid,
                sessionTokenId = oldSessionToken.id,
                expiryAt = LocalDateTime.now().plusDays(30),
            )

        `when`(refreshTokenRepository.findByTokenHash(oldRefreshTokenHash)).thenReturn(Optional.of(existingRefreshToken))
        `when`(userRepository.findById(user.uuid)).thenReturn(Optional.of(user))
        `when`(sessionTokenRepository.save(any<SessionToken>())).thenAnswer { it.arguments[0] }
        `when`(refreshTokenRepository.save(any<RefreshToken>())).thenAnswer { it.arguments[0] }
        `when`(mongoTemplate.findAndRemove(any(), eq(RefreshToken::class.java))).thenReturn(existingRefreshToken)

        val result = authService.refresh(oldRefreshTokenStr)

        assertThat(result.accessToken).isNotNull()
        assertThat(result.refreshToken).isNotNull()
        assertThat(result.accessToken).isNotEqualTo(oldSessionToken.token)
        assertThat(result.refreshToken).isNotEqualTo(oldRefreshTokenStr)

        verify(sessionTokenRepository).deleteById(oldSessionToken.id)
    }

    @Test
    fun `refresh should throw exception with expired refresh token`() {
        val expiredTokenStr = "expired-refresh-token"
        val expiredTokenHash = "hashed-$expiredTokenStr"
        val expiredRefreshToken =
            RefreshToken(
                tokenHash = expiredTokenHash,
                userId = "user-123",
                sessionTokenId = "session-123",
                expiryAt = LocalDateTime.now().minusHours(1),
            )

        `when`(refreshTokenRepository.findByTokenHash(expiredTokenHash)).thenReturn(Optional.of(expiredRefreshToken))

        val exception =
            assertThrows<IllegalArgumentException> {
                authService.refresh(expiredTokenStr)
            }
        assertThat(exception.message).isEqualTo("Invalid or expired refresh token")
    }

    @Test
    fun `refresh should throw exception with invalid refresh token`() {
        val invalidTokenStr = "invalid-refresh-token"
        val invalidTokenHash = "hashed-$invalidTokenStr"

        `when`(refreshTokenRepository.findByTokenHash(invalidTokenHash)).thenReturn(Optional.empty())

        val exception =
            assertThrows<IllegalArgumentException> {
                authService.refresh(invalidTokenStr)
            }
        assertThat(exception.message).isEqualTo("Invalid or expired refresh token")
    }

    @Test
    fun `refresh should throw exception when user is inactive`() {
        val refreshTokenStr = "valid-refresh-token"
        val refreshTokenHash = "hashed-$refreshTokenStr"
        val inactiveUser = createMockUser().copy(isActive = false)
        val existingRefreshToken =
            RefreshToken(
                tokenHash = refreshTokenHash,
                userId = inactiveUser.uuid,
                sessionTokenId = "session-123",
                expiryAt = LocalDateTime.now().plusDays(30),
            )

        `when`(refreshTokenRepository.findByTokenHash(refreshTokenHash)).thenReturn(Optional.of(existingRefreshToken))
        `when`(userRepository.findById(inactiveUser.uuid)).thenReturn(Optional.of(inactiveUser))

        val exception =
            assertThrows<IllegalArgumentException> {
                authService.refresh(refreshTokenStr)
            }
        assertThat(exception.message).isEqualTo("User account is inactive")
    }

    @Test
    fun `logout should do nothing when session token is not found`() {
        val token = "test-token-123"

        `when`(sessionTokenRepository.findByToken(token)).thenReturn(Optional.empty())

        authService.logout(token)

        verify(sessionTokenRepository, times(1)).findByToken(token)
        verify(refreshTokenRepository, never()).deleteBySessionTokenId(any())
        verify(sessionTokenRepository, never()).deleteByToken(any())
    }

    @Test
    fun `logout should delete both session token and refresh token`() {
        val token = "test-token-123"
        val sessionToken = createMockSessionToken()

        `when`(sessionTokenRepository.findByToken(token)).thenReturn(Optional.of(sessionToken))

        authService.logout(token)

        verify(refreshTokenRepository, times(1)).deleteBySessionTokenId(sessionToken.id)
        verify(sessionTokenRepository, times(1)).deleteByToken(token)
    }

    @Test
    fun `listSessions should return only non-expired sessions`() {
        val userId = "user-123"
        val activeSession = SessionToken(id = "s1", token = "t1", userId = userId, expiryAt = LocalDateTime.now().plusHours(12))
        `when`(sessionTokenRepository.findByUserIdAndExpiryAtAfter(eq(userId), any())).thenReturn(listOf(activeSession))

        val result = authService.listSessions(userId)

        assertThat(result.size).isEqualTo(1)
        assertThat(result[0].id).isEqualTo("s1")
    }

    @Test
    fun `revokeSession should delete session and its refresh token`() {
        val userId = "user-123"
        val session = SessionToken(id = "s1", token = "t1", userId = userId, expiryAt = LocalDateTime.now().plusHours(12))

        `when`(sessionTokenRepository.findById("s1")).thenReturn(Optional.of(session))

        authService.revokeSession(userId, "s1", "other-token")

        verify(refreshTokenRepository).deleteBySessionTokenId("s1")
        verify(sessionTokenRepository).deleteById("s1")
    }

    @Test
    fun `revokeSession should throw when session belongs to different user`() {
        val session = SessionToken(id = "s1", token = "t1", userId = "other-user", expiryAt = LocalDateTime.now().plusHours(12))

        `when`(sessionTokenRepository.findById("s1")).thenReturn(Optional.of(session))

        val exception =
            assertThrows<IllegalArgumentException> {
                authService.revokeSession("user-123", "s1", "other-token")
            }
        assertThat(exception.message).isEqualTo("Session not found")
    }

    @Test
    fun `revokeSession should throw when attempting to revoke the current session`() {
        val userId = "user-123"
        val currentToken = "current-token"
        val session = SessionToken(id = "s1", token = currentToken, userId = userId, expiryAt = LocalDateTime.now().plusHours(12))

        `when`(sessionTokenRepository.findById("s1")).thenReturn(Optional.of(session))

        val exception =
            assertThrows<IllegalStateException> {
                authService.revokeSession(userId, "s1", currentToken)
            }
        assertThat(exception.message).isEqualTo("Cannot revoke the current session")
    }

    @Test
    fun `revokeOtherSessions should keep current session and delete others in bulk`() {
        val userId = "user-123"
        val currentToken = "current-token"
        val otherSessions =
            listOf(
                SessionToken(id = "s2", token = "other-token", userId = userId, expiryAt = LocalDateTime.now().plusHours(12)),
            )
        `when`(sessionTokenRepository.findByUserIdAndTokenNot(userId, currentToken)).thenReturn(otherSessions)

        authService.revokeOtherSessions(userId, currentToken)

        verify(refreshTokenRepository).deleteBySessionTokenIdIn(listOf("s2"))
        verify(sessionTokenRepository).deleteAllById(listOf("s2"))
    }

    @Test
    fun `changePassword should update password with valid current password`() {
        val user = createMockUser()
        `when`(passwordEncoder.matches("currentPass", user.passwordHash)).thenReturn(true)
        `when`(passwordEncoder.encode("NewSecurePass123!")).thenReturn("newEncodedPassword")
        `when`(userRepository.save(any<User>())).thenAnswer { it.arguments[0] }

        authService.changePassword(user, "currentPass", "NewSecurePass123!")

        val userCaptor = argumentCaptor<User>()
        verify(userRepository).save(userCaptor.capture())
        assertThat(userCaptor.firstValue.passwordHash).isEqualTo("newEncodedPassword")
        verify(sessionTokenRepository).deleteByUserId(user.uuid)
        verify(refreshTokenRepository).deleteByUserId(user.uuid)
    }

    @Test
    fun `changePassword should throw when current password is incorrect`() {
        val user = createMockUser()
        `when`(passwordEncoder.matches("wrongPass", user.passwordHash)).thenReturn(false)

        val exception =
            assertThrows<IllegalArgumentException> {
                authService.changePassword(user, "wrongPass", "NewSecurePass123!")
            }
        assertThat(exception.message).isEqualTo("Current password is incorrect")
    }

    @Test
    fun `changePassword should throw when new password is same as current`() {
        val user = createMockUser()
        `when`(passwordEncoder.matches("samePass", user.passwordHash)).thenReturn(true)

        val exception =
            assertThrows<IllegalArgumentException> {
                authService.changePassword(user, "samePass", "samePass")
            }
        assertThat(exception.message).isEqualTo("New password must be different from current password")
    }

    @Test
    fun `forgotPassword should return reset token for valid email`() {
        val user = createMockUser()
        `when`(userRepository.findByEmail(user.email)).thenReturn(Optional.of(user))
        `when`(passwordResetTokenRepository.save(any<PasswordResetToken>())).thenAnswer { it.arguments[0] }

        val result = authService.forgotPassword(user.email)

        assertThat(result).isNotNull().isNotEmpty()
        verify(passwordResetTokenRepository).deleteByUserId(user.uuid)
        verify(passwordResetTokenRepository).save(any<PasswordResetToken>())
    }

    @Test
    fun `forgotPassword should return null for unknown email`() {
        `when`(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty())

        val result = authService.forgotPassword("unknown@example.com")

        assertThat(result).isNull()
        verify(passwordResetTokenRepository, never()).save(any())
    }

    @Test
    fun `forgotPassword should return null for inactive user`() {
        val inactiveUser = createMockUser().copy(isActive = false)
        `when`(userRepository.findByEmail(inactiveUser.email)).thenReturn(Optional.of(inactiveUser))

        val result = authService.forgotPassword(inactiveUser.email)

        assertThat(result).isNull()
        verify(passwordResetTokenRepository, never()).save(any())
    }

    @Test
    fun `resetPassword should update password and invalidate all sessions`() {
        val user = createMockUser()
        val resetToken =
            PasswordResetToken(
                tokenHash = "hashed-valid-token",
                userId = user.uuid,
                expiryAt = LocalDateTime.now().plusMinutes(30),
            )

        `when`(passwordResetTokenRepository.findByTokenHash("hashed-valid-token")).thenReturn(Optional.of(resetToken))
        `when`(mongoTemplate.findAndRemove(any(), eq(PasswordResetToken::class.java))).thenReturn(resetToken)
        `when`(userRepository.findById(user.uuid)).thenReturn(Optional.of(user))
        `when`(passwordEncoder.encode("NewPassword123!")).thenReturn("newEncodedPass")
        `when`(userRepository.save(any<User>())).thenAnswer { it.arguments[0] }

        authService.resetPassword("valid-token", "NewPassword123!")

        verify(userRepository).save(any<User>())
        verify(sessionTokenRepository).deleteByUserId(user.uuid)
        verify(refreshTokenRepository).deleteByUserId(user.uuid)
    }

    @Test
    fun `resetPassword should throw for invalid token`() {
        `when`(passwordResetTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty())

        val exception =
            assertThrows<IllegalArgumentException> {
                authService.resetPassword("invalid-token", "NewPassword123!")
            }
        assertThat(exception.message).isEqualTo("Invalid or expired reset token")
    }

    @Test
    fun `resetPassword should throw for expired token without consuming it permanently`() {
        val expiredToken =
            PasswordResetToken(
                tokenHash = "hashed-expired-token",
                userId = "user-123",
                expiryAt = LocalDateTime.now().minusMinutes(10),
            )

        `when`(passwordResetTokenRepository.findByTokenHash("hashed-expired-token"))
            .thenReturn(Optional.of(expiredToken))

        val exception =
            assertThrows<IllegalArgumentException> {
                authService.resetPassword("expired-token", "NewPassword123!")
            }
        assertThat(exception.message).isEqualTo("Invalid or expired reset token")
        verify(passwordResetTokenRepository).deleteById(expiredToken.id)
        verify(mongoTemplate, never()).findAndRemove(any(), eq(PasswordResetToken::class.java))
    }

    private fun createValidRegisterRequest() =
        RegisterRequest(
            username = "testuser",
            password = "SecurePass123!",
            email = "test@example.com",
            firstName = "Test",
            lastName = "User",
            orgName = "Test Organization",
            orgSlug = "test-org",
            orgDescription = "A test organization",
            orgBaseCurrency = "USD",
            orgFiscalYearStart = LocalDateTime.of(2024, 1, 1, 0, 0),
            orgTimezone = "UTC",
            orgLegalName = "Test Organization LLC",
            orgTradeName = "Test Org",
        )

    @Test
    fun `switchOrganization should return new token pair scoped to target org`() {
        val user =
            createMockUser().copy(
                roleAssignments =
                    listOf(
                        RoleAssignment("OWNER", "org-123"),
                        RoleAssignment("MEMBER", "org-456"),
                    ),
            )
        val targetOrg = createMockOrganization().copy(uuid = "org-456")

        `when`(organizationRepository.findById("org-456")).thenReturn(Optional.of(targetOrg))
        `when`(sessionTokenRepository.save(any<SessionToken>())).thenAnswer { it.arguments[0] }
        `when`(refreshTokenRepository.save(any<RefreshToken>())).thenAnswer { it.arguments[0] }

        val result = authService.switchOrganization(user, "org-456")

        assertThat(result.organizationId).isEqualTo("org-456")
        assertThat(result.roles).isEqualTo(listOf("MEMBER"))
        assertThat(result.accessToken).isNotNull()
        assertThat(result.refreshToken).isNotNull()

        val sessionCaptor = argumentCaptor<SessionToken>()
        verify(sessionTokenRepository).save(sessionCaptor.capture())
        assertThat(sessionCaptor.firstValue.organizationId).isEqualTo("org-456")
    }

    @Test
    fun `switchOrganization should throw when user has no role in target org`() {
        val user = createMockUser()

        val exception =
            assertThrows<IllegalArgumentException> {
                authService.switchOrganization(user, "org-999")
            }
        assertThat(exception.message).isEqualTo("You do not have access to this organization")
    }

    @Test
    fun `switchOrganization should throw when org not found`() {
        val user =
            createMockUser().copy(
                roleAssignments =
                    listOf(
                        RoleAssignment("MEMBER", "org-123"),
                        RoleAssignment("MEMBER", "org-missing"),
                    ),
            )

        `when`(organizationRepository.findById("org-missing")).thenReturn(Optional.empty())

        val exception =
            assertThrows<IllegalArgumentException> {
                authService.switchOrganization(user, "org-missing")
            }
        assertThat(exception.message).isEqualTo("Organization not found")
    }

    @Test
    fun `switchOrganization should throw when org is inactive`() {
        val user =
            createMockUser().copy(
                roleAssignments =
                    listOf(
                        RoleAssignment("MEMBER", "org-123"),
                        RoleAssignment("MEMBER", "org-inactive"),
                    ),
            )
        val inactiveOrg = createMockOrganization().copy(uuid = "org-inactive", isActive = false)

        `when`(organizationRepository.findById("org-inactive")).thenReturn(Optional.of(inactiveOrg))

        val exception =
            assertThrows<IllegalArgumentException> {
                authService.switchOrganization(user, "org-inactive")
            }
        assertThat(exception.message).isEqualTo("Organization is not active")
    }

    @Test
    fun `listUserOrganizations should return orgs with current indicator`() {
        val user =
            createMockUser().copy(
                roleAssignments =
                    listOf(
                        RoleAssignment("OWNER", "org-123"),
                        RoleAssignment("MEMBER", "org-456"),
                    ),
            )
        val org1 = createMockOrganization().copy(uuid = "org-123", name = "Org One", orgSlug = "org-one")
        val org2 = createMockOrganization().copy(uuid = "org-456", name = "Org Two", orgSlug = "org-two")

        `when`(organizationRepository.findAllById(listOf("org-123", "org-456"))).thenReturn(listOf(org1, org2))

        val result = authService.listUserOrganizations(user, "org-123")

        assertThat(result.size).isEqualTo(2)
        val current = result.first { it.isCurrent }
        assertThat(current.organizationId).isEqualTo("org-123")
        assertThat(current.roles).isEqualTo(listOf("OWNER"))

        val other = result.first { !it.isCurrent }
        assertThat(other.organizationId).isEqualTo("org-456")
        assertThat(other.roles).isEqualTo(listOf("MEMBER"))
    }

    @Test
    fun `listUserOrganizations should include inactive orgs with isActive false`() {
        val user =
            createMockUser().copy(
                roleAssignments =
                    listOf(
                        RoleAssignment("OWNER", "org-123"),
                        RoleAssignment("MEMBER", "org-inactive"),
                    ),
            )
        val activeOrg = createMockOrganization().copy(uuid = "org-123")
        val inactiveOrg = createMockOrganization().copy(uuid = "org-inactive", isActive = false)

        `when`(organizationRepository.findAllById(listOf("org-123", "org-inactive"))).thenReturn(listOf(activeOrg, inactiveOrg))

        val result = authService.listUserOrganizations(user, "org-123")

        assertThat(result.size).isEqualTo(2)
        val active = result.first { it.organizationId == "org-123" }
        assertThat(active.isActive).isTrue()
        val inactive = result.first { it.organizationId == "org-inactive" }
        assertThat(inactive.isActive).isFalse()
    }

    private fun createMockOrganization() =
        Organizations(
            uuid = "org-123",
            name = "Test Organization",
            orgSlug = "test-org",
            description = "Test description",
            baseCurrency = "USD",
            fiscalYearStart = LocalDateTime.of(2024, 1, 1, 0, 0),
            timezone = "UTC",
            legalName = "Test Organization LLC",
            tradeName = "Test Org",
        )

    private fun createMockUser() =
        User(
            uuid = "user-123",
            username = "testuser",
            email = "test@example.com",
            firstName = "Test",
            lastName = "User",
            passwordHash = "encodedPassword",
            organizationId = "org-123",
            roleAssignments = listOf(RoleAssignment("MEMBER", "org-123")),
        )

    private fun createMockSessionToken() =
        SessionToken(
            id = "token-123",
            token = "generated-token",
            userId = "user-123",
            expiryAt = LocalDateTime.now().plusHours(24),
        )
}
