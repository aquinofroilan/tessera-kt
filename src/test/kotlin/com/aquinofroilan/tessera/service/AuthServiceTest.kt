package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.LoginRequest
import com.aquinofroilan.tessera.dto.RegisterRequest
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
import com.aquinofroilan.tessera.repository.OrganizationRepository
import com.aquinofroilan.tessera.repository.PasswordResetTokenRepository
import com.aquinofroilan.tessera.repository.RefreshTokenRepository
import com.aquinofroilan.tessera.repository.SessionTokenRepository
import com.aquinofroilan.tessera.repository.UserRepository
import com.aquinofroilan.tessera.util.TokenHasher
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
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDateTime
import java.time.ZoneOffset
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
    private lateinit var jdbcTemplate: JdbcTemplate
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
        jdbcTemplate = mock(JdbcTemplate::class.java)
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
                jdbcTemplate = jdbcTemplate,
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
                uuid = java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"),
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
                uuid = java.util.UUID.fromString("3a01035d-c5db-3981-bf73-f18b3a0c1df9"),
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
            .thenThrow(DuplicateKeyException("E11000 duplicate key error collection: tessera.users index: username"))

        val exception =
            assertThrows<BusinessRuleException> {
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
            .thenThrow(DuplicateKeyException("E11000 duplicate key error collection: tessera.users index: email"))

        val exception =
            assertThrows<BusinessRuleException> {
                authService.register(request)
            }
        assertThat(exception.message).isEqualTo("Email already exists")
    }

    @Test
    fun `register should throw exception when organization slug already exists`() {
        val request = createValidRegisterRequest()
        `when`(organizationRepository.save(any<Organizations>()))
            .thenThrow(DuplicateKeyException("E11000 duplicate key error collection: tessera.organizations index: orgSlug"))

        val exception =
            assertThrows<BusinessRuleException> {
                authService.register(request)
            }
        assertThat(exception.message).isEqualTo("Organization slug already exists")
        verify(accountService, never()).seedDefaultAccounts(any())
    }

    @Test
    fun `register should throw exception when organization name already exists`() {
        val request = createValidRegisterRequest()
        `when`(organizationRepository.save(any<Organizations>()))
            .thenThrow(DuplicateKeyException("E11000 duplicate key error collection: tessera.organizations index: name"))

        val exception =
            assertThrows<BusinessRuleException> {
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
                uuid = java.util.UUID.fromString("3a01035d-c5db-3981-bf73-f18b3a0c1df9"),
                username = request.username,
                email = "test@example.com",
                firstName = "Test",
                lastName = "User",
                passwordHash = "encodedPassword",
                organizationId = java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"),
                roleAssignments =
                    listOf(
                        RoleAssignment("MEMBER", java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d")),
                        RoleAssignment("ADMIN", java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d")),
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

        val expectedRefreshExpiry = LocalDateTime.now(ZoneOffset.UTC).plusDays(30)
        val actualRefreshExpiry = LocalDateTime.parse(result.refreshTokenExpiresAt)
        assertThat(actualRefreshExpiry).isAfter(expectedRefreshExpiry.minusMinutes(1))
        assertThat(actualRefreshExpiry).isBefore(expectedRefreshExpiry.plusMinutes(1))
    }

    @Test
    fun `login should throw exception with invalid username`() {
        val request = LoginRequest(username = "nonexistent", password = "password123")
        `when`(userRepository.findByUsername(request.username)).thenReturn(Optional.empty())

        val exception =
            assertThrows<AuthenticationException> {
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
            assertThrows<AuthenticationException> {
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

        val expectedExpiry = LocalDateTime.now(ZoneOffset.UTC).plusHours(24)
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
        val inactiveUser = createMockUser().apply { isActive = false }

        `when`(userRepository.findByUsername(request.username)).thenReturn(Optional.of(inactiveUser))

        val exception =
            assertThrows<AuthenticationException> {
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
                id = java.util.UUID.fromString("f0819e9d-eb2c-39d0-89eb-bc6099c0126b"),
                tokenHash = oldRefreshTokenHash,
                userId = user.uuid,
                sessionTokenId = oldSessionToken.id,
                expiryAt = LocalDateTime.now(ZoneOffset.UTC).plusDays(30),
            )

        `when`(refreshTokenRepository.findByTokenHash(oldRefreshTokenHash)).thenReturn(Optional.of(existingRefreshToken))
        `when`(userRepository.findById(user.uuid)).thenReturn(Optional.of(user))
        `when`(sessionTokenRepository.save(any<SessionToken>())).thenAnswer { it.arguments[0] }
        `when`(refreshTokenRepository.save(any<RefreshToken>())).thenAnswer { it.arguments[0] }
        `when`(jdbcTemplate.update(any<String>(), eq(oldRefreshTokenHash))).thenReturn(1)

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
                userId = java.util.UUID.fromString("3a01035d-c5db-3981-bf73-f18b3a0c1df9"),
                sessionTokenId = java.util.UUID.fromString("18734c39-05d7-34a5-9b8b-b9478597bffa"),
                expiryAt = LocalDateTime.now(ZoneOffset.UTC).minusHours(1),
            )

        `when`(refreshTokenRepository.findByTokenHash(expiredTokenHash)).thenReturn(Optional.of(expiredRefreshToken))

        val exception =
            assertThrows<AuthenticationException> {
                authService.refresh(expiredTokenStr)
            }
        assertThat(exception.message).isEqualTo("Invalid or expired refresh token")
    }

    @Test
    fun `refresh should throw exception with invalid refresh token`() {
        val invalidTokenStr = "99f9014b-4737-3010-80ac-4ed761491cfc"
        val invalidTokenHash = "f1f540a6-c40c-340b-a0f8-e3b0fcb7d805"

        `when`(refreshTokenRepository.findByTokenHash(invalidTokenHash)).thenReturn(Optional.empty())

        val exception =
            assertThrows<AuthenticationException> {
                authService.refresh(invalidTokenStr)
            }
        assertThat(exception.message).isEqualTo("Invalid or expired refresh token")
    }

    @Test
    fun `refresh should throw exception when user is inactive`() {
        val refreshTokenStr = "valid-refresh-token"
        val refreshTokenHash = "hashed-$refreshTokenStr"
        val inactiveUser = createMockUser().apply { isActive = false }
        val existingRefreshToken =
            RefreshToken(
                tokenHash = refreshTokenHash,
                userId = inactiveUser.uuid,
                sessionTokenId = java.util.UUID.fromString("18734c39-05d7-34a5-9b8b-b9478597bffa"),
                expiryAt = LocalDateTime.now(ZoneOffset.UTC).plusDays(30),
            )

        `when`(refreshTokenRepository.findByTokenHash(refreshTokenHash)).thenReturn(Optional.of(existingRefreshToken))
        `when`(userRepository.findById(inactiveUser.uuid)).thenReturn(Optional.of(inactiveUser))

        val exception =
            assertThrows<AuthenticationException> {
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
        val userId = java.util.UUID.fromString("3a01035d-c5db-3981-bf73-f18b3a0c1df9")
        val activeSession =
            SessionToken(id = java.util.UUID.fromString("9a95230c-f3ef-320a-9d34-fc4c73ce5ce3"), token = "t1", userId = userId, expiryAt = LocalDateTime.now(ZoneOffset.UTC).plusHours(12))
        `when`(sessionTokenRepository.findByUserIdAndExpiryAtAfter(eq(userId), any())).thenReturn(listOf(activeSession))

        val result = authService.listSessions(userId)

        assertThat(result.size).isEqualTo(1)
        assertThat(result[0].id).isEqualTo(java.util.UUID.fromString("9a95230c-f3ef-320a-9d34-fc4c73ce5ce3"))
    }

    @Test
    fun `revokeSession should delete session and its refresh token`() {
        val userId = java.util.UUID.fromString("3a01035d-c5db-3981-bf73-f18b3a0c1df9")
        val session = SessionToken(id = java.util.UUID.fromString("9a95230c-f3ef-320a-9d34-fc4c73ce5ce3"), token = "t1", userId = userId, expiryAt = LocalDateTime.now(ZoneOffset.UTC).plusHours(12))

        `when`(sessionTokenRepository.findById(java.util.UUID.fromString("9a95230c-f3ef-320a-9d34-fc4c73ce5ce3"))).thenReturn(Optional.of(session))

        authService.revokeSession(userId, java.util.UUID.fromString("9a95230c-f3ef-320a-9d34-fc4c73ce5ce3"), "5d3a39eb-7f53-38bd-9285-b709451d2bb5")

        verify(refreshTokenRepository).deleteBySessionTokenId(java.util.UUID.fromString("9a95230c-f3ef-320a-9d34-fc4c73ce5ce3"))
        verify(sessionTokenRepository).deleteById(java.util.UUID.fromString("9a95230c-f3ef-320a-9d34-fc4c73ce5ce3"))
    }

    @Test
    fun `revokeSession should throw when session belongs to different user`() {
        val session =
            SessionToken(id = java.util.UUID.fromString("9a95230c-f3ef-320a-9d34-fc4c73ce5ce3"), token = "t1", userId = java.util.UUID.fromString("10a8c040-b348-34f9-b495-1d1c714ae089"), expiryAt = LocalDateTime.now(ZoneOffset.UTC).plusHours(12))

        `when`(sessionTokenRepository.findById(java.util.UUID.fromString("9a95230c-f3ef-320a-9d34-fc4c73ce5ce3"))).thenReturn(Optional.of(session))

        val exception =
            assertThrows<ResourceNotFoundException> {
                authService.revokeSession(java.util.UUID.fromString("3a01035d-c5db-3981-bf73-f18b3a0c1df9"), java.util.UUID.fromString("9a95230c-f3ef-320a-9d34-fc4c73ce5ce3"), "5d3a39eb-7f53-38bd-9285-b709451d2bb5")
            }
        assertThat(exception.message).isEqualTo("Session not found")
    }

    @Test
    fun `revokeSession should throw when attempting to revoke the current session`() {
        val userId = java.util.UUID.fromString("3a01035d-c5db-3981-bf73-f18b3a0c1df9")
        val currentToken = "current-token"
        val session =
            SessionToken(id = java.util.UUID.fromString("9a95230c-f3ef-320a-9d34-fc4c73ce5ce3"), token = currentToken, userId = userId, expiryAt = LocalDateTime.now(ZoneOffset.UTC).plusHours(12))

        `when`(sessionTokenRepository.findById(java.util.UUID.fromString("9a95230c-f3ef-320a-9d34-fc4c73ce5ce3"))).thenReturn(Optional.of(session))

        val exception =
            assertThrows<BusinessRuleException> {
                authService.revokeSession(userId, java.util.UUID.fromString("9a95230c-f3ef-320a-9d34-fc4c73ce5ce3"), currentToken)
            }
        assertThat(exception.message).isEqualTo("Cannot revoke the current session")
    }

    @Test
    fun `revokeOtherSessions should keep current session and delete others in bulk`() {
        val userId = java.util.UUID.fromString("3a01035d-c5db-3981-bf73-f18b3a0c1df9")
        val currentToken = "current-token"
        val otherSessions =
            listOf(
                SessionToken(id = java.util.UUID.fromString("077439bc-04b8-3d51-a3f4-7b157fbc8ea0"), token = "other-token", userId = userId, expiryAt = LocalDateTime.now(ZoneOffset.UTC).plusHours(12)),
            )
        `when`(sessionTokenRepository.findByUserIdAndTokenNot(userId, currentToken)).thenReturn(otherSessions)

        authService.revokeOtherSessions(userId, currentToken)

        verify(refreshTokenRepository).deleteBySessionTokenIdIn(listOf(java.util.UUID.fromString("077439bc-04b8-3d51-a3f4-7b157fbc8ea0")))
        verify(sessionTokenRepository).deleteAllById(listOf(java.util.UUID.fromString("077439bc-04b8-3d51-a3f4-7b157fbc8ea0")))
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
            assertThrows<BusinessRuleException> {
                authService.changePassword(user, "wrongPass", "NewSecurePass123!")
            }
        assertThat(exception.message).isEqualTo("Current password is incorrect")
    }

    @Test
    fun `changePassword should throw when new password is same as current`() {
        val user = createMockUser()
        `when`(passwordEncoder.matches("samePass", user.passwordHash)).thenReturn(true)

        val exception =
            assertThrows<BusinessRuleException> {
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
        val inactiveUser = createMockUser().apply { isActive = false }
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
                expiryAt = LocalDateTime.now(ZoneOffset.UTC).plusMinutes(30),
            )

        `when`(passwordResetTokenRepository.findByTokenHash("hashed-valid-token")).thenReturn(Optional.of(resetToken))
        `when`(jdbcTemplate.update(any<String>(), eq("hashed-valid-token"))).thenReturn(1)
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
            assertThrows<BusinessRuleException> {
                authService.resetPassword("invalid-token", "NewPassword123!")
            }
        assertThat(exception.message).isEqualTo("Invalid or expired reset token")
    }

    @Test
    fun `resetPassword should throw for expired token without consuming it permanently`() {
        val expiredToken =
            PasswordResetToken(
                tokenHash = "hashed-expired-token",
                userId = java.util.UUID.fromString("3a01035d-c5db-3981-bf73-f18b3a0c1df9"),
                expiryAt = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(10),
            )

        `when`(passwordResetTokenRepository.findByTokenHash("hashed-expired-token"))
            .thenReturn(Optional.of(expiredToken))

        val exception =
            assertThrows<BusinessRuleException> {
                authService.resetPassword("expired-token", "NewPassword123!")
            }
        assertThat(exception.message).isEqualTo("Invalid or expired reset token")
        verify(passwordResetTokenRepository).deleteById(expiredToken.id)
        verify(jdbcTemplate, never()).update(any<String>(), eq("hashed-expired-token"))
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
            createMockUser().apply {
                roleAssignments =
                    listOf(
                        RoleAssignment("OWNER", java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d")),
                        RoleAssignment("MEMBER", java.util.UUID.fromString("8576b8f7-dd04-3e57-b849-081b3776f223")),
                    )
            }
        val targetOrg = createMockOrganization().apply { uuid = java.util.UUID.fromString("8576b8f7-dd04-3e57-b849-081b3776f223") }

        `when`(organizationRepository.findById(java.util.UUID.fromString("8576b8f7-dd04-3e57-b849-081b3776f223"))).thenReturn(Optional.of(targetOrg))
        `when`(sessionTokenRepository.save(any<SessionToken>())).thenAnswer { it.arguments[0] }
        `when`(refreshTokenRepository.save(any<RefreshToken>())).thenAnswer { it.arguments[0] }

        val result = authService.switchOrganization(user, java.util.UUID.fromString("8576b8f7-dd04-3e57-b849-081b3776f223"))

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
            assertThrows<BusinessRuleException> {
                authService.switchOrganization(user, java.util.UUID.fromString("800a923a-74f1-3b92-b740-74c9c41c47f7"))
            }
        assertThat(exception.message).isEqualTo("You do not have access to this organization")
    }

    @Test
    fun `switchOrganization should throw when org not found`() {
        val user =
            createMockUser().apply {
                roleAssignments =
                    listOf(
                        RoleAssignment("MEMBER", java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d")),
                        RoleAssignment("MEMBER", java.util.UUID.fromString("87d38354-2a17-3485-b3bb-33c1a83acb7a")),
                    )
            }

        `when`(organizationRepository.findById(java.util.UUID.fromString("87d38354-2a17-3485-b3bb-33c1a83acb7a"))).thenReturn(Optional.empty())

        val exception =
            assertThrows<ResourceNotFoundException> {
                authService.switchOrganization(user, java.util.UUID.fromString("87d38354-2a17-3485-b3bb-33c1a83acb7a"))
            }
        assertThat(exception.message).isEqualTo("Organization not found")
    }

    @Test
    fun `switchOrganization should throw when org is inactive`() {
        val user =
            createMockUser().apply {
                roleAssignments =
                    listOf(
                        RoleAssignment("MEMBER", java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d")),
                        RoleAssignment("MEMBER", java.util.UUID.fromString("e679dc56-27ba-357b-9d96-f59411774170")),
                    )
            }
        val inactiveOrg =
            createMockOrganization().apply {
                uuid = java.util.UUID.fromString("e679dc56-27ba-357b-9d96-f59411774170")
                isActive = false
            }

        `when`(organizationRepository.findById(java.util.UUID.fromString("e679dc56-27ba-357b-9d96-f59411774170"))).thenReturn(Optional.of(inactiveOrg))

        val exception =
            assertThrows<BusinessRuleException> {
                authService.switchOrganization(user, java.util.UUID.fromString("e679dc56-27ba-357b-9d96-f59411774170"))
            }
        assertThat(exception.message).isEqualTo("Organization is not active")
    }

    @Test
    fun `listUserOrganizations should return orgs with current indicator`() {
        val user =
            createMockUser().apply {
                roleAssignments =
                    listOf(
                        RoleAssignment("OWNER", java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d")),
                        RoleAssignment("MEMBER", java.util.UUID.fromString("8576b8f7-dd04-3e57-b849-081b3776f223")),
                    )
            }
        val org1 =
            createMockOrganization().apply {
                uuid = java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d")
                name = "Org One"
                orgSlug = "org-one"
            }
        val org2 =
            createMockOrganization().apply {
                uuid = java.util.UUID.fromString("8576b8f7-dd04-3e57-b849-081b3776f223")
                name = "Org Two"
                orgSlug = "org-two"
            }

        `when`(organizationRepository.findAllById(listOf(java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"), java.util.UUID.fromString("8576b8f7-dd04-3e57-b849-081b3776f223")))).thenReturn(listOf(org1, org2))

        val result = authService.listUserOrganizations(user, java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"))

        assertThat(result.size).isEqualTo(2)
        val current = result.first { it.isCurrent }
        assertThat(current.organizationId).isEqualTo(java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"))
        assertThat(current.roles).isEqualTo(listOf("OWNER"))

        val other = result.first { !it.isCurrent }
        assertThat(other.organizationId).isEqualTo(java.util.UUID.fromString("8576b8f7-dd04-3e57-b849-081b3776f223"))
        assertThat(other.roles).isEqualTo(listOf("MEMBER"))
    }

    @Test
    fun `listUserOrganizations should include inactive orgs with isActive false`() {
        val user =
            createMockUser().apply {
                roleAssignments =
                    listOf(
                        RoleAssignment("OWNER", java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d")),
                        RoleAssignment("MEMBER", java.util.UUID.fromString("e679dc56-27ba-357b-9d96-f59411774170")),
                    )
            }
        val activeOrg = createMockOrganization().apply { uuid = java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d") }
        val inactiveOrg =
            createMockOrganization().apply {
                uuid = java.util.UUID.fromString("e679dc56-27ba-357b-9d96-f59411774170")
                isActive = false
            }

        `when`(organizationRepository.findAllById(listOf(java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"), java.util.UUID.fromString("e679dc56-27ba-357b-9d96-f59411774170")))).thenReturn(listOf(activeOrg, inactiveOrg))

        val result = authService.listUserOrganizations(user, java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"))

        assertThat(result.size).isEqualTo(2)
        val active = result.first { it.organizationId == java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d") }
        assertThat(active.isActive).isTrue()
        val inactive = result.first { it.organizationId == java.util.UUID.fromString("e679dc56-27ba-357b-9d96-f59411774170") }
        assertThat(inactive.isActive).isFalse()
    }

    private fun createMockOrganization() =
        Organizations(
            uuid = java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"),
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
            uuid = java.util.UUID.fromString("3a01035d-c5db-3981-bf73-f18b3a0c1df9"),
            username = "testuser",
            email = "test@example.com",
            firstName = "Test",
            lastName = "User",
            passwordHash = "encodedPassword",
            organizationId = java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"),
            roleAssignments = listOf(RoleAssignment("MEMBER", java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"))),
        )

    private fun createMockSessionToken() =
        SessionToken(
            id = java.util.UUID.fromString("d33b440a-b021-3e58-824f-1c4750763da4"),
            token = "generated-token",
            userId = java.util.UUID.fromString("3a01035d-c5db-3981-bf73-f18b3a0c1df9"),
            expiryAt = LocalDateTime.now(ZoneOffset.UTC).plusHours(24),
        )
}
