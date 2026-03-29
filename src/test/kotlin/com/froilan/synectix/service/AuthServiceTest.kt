package com.froilan.synectix.service

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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
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
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AuthServiceTest {

    private lateinit var authService: AuthService
    private lateinit var userRepository: UserRepository
    private lateinit var organizationRepository: OrganizationRepository
    private lateinit var sessionTokenRepository: SessionTokenRepository
    private lateinit var refreshTokenRepository: RefreshTokenRepository
    private lateinit var mongoTemplate: MongoTemplate
    private lateinit var passwordEncoder: PasswordEncoder

    @BeforeEach
    fun setup() {
        userRepository = mock(UserRepository::class.java)
        organizationRepository = mock(OrganizationRepository::class.java)
        sessionTokenRepository = mock(SessionTokenRepository::class.java)
        refreshTokenRepository = mock(RefreshTokenRepository::class.java)
        mongoTemplate = mock(MongoTemplate::class.java)
        passwordEncoder = mock(PasswordEncoder::class.java)

        authService = AuthService(
            userRepository = userRepository,
            organizationRepository = organizationRepository,
            sessionTokenRepository = sessionTokenRepository,
            refreshTokenRepository = refreshTokenRepository,
            mongoTemplate = mongoTemplate,
            passwordEncoder = passwordEncoder,
            tokenValidityMs = 86400000L,
            refreshTokenValidityMs = 2592000000L,
        )
    }


    @Test
    fun `register should create organization and user successfully`() {
        val request = createValidRegisterRequest()
        val encodedPassword = "encodedPassword123"
        val savedOrg = Organizations(
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
        val savedUser = User(
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

        assertNotNull(result)
        assertEquals(request.username, result.username)
        assertEquals(request.email, result.email)
        assertEquals(savedOrg.uuid, result.organizationId)

        verify(organizationRepository, times(1)).save(any<Organizations>())

        val userCaptor = argumentCaptor<User>()
        verify(userRepository, times(1)).save(userCaptor.capture())
        assertEquals(encodedPassword, userCaptor.firstValue.passwordHash)
        assertEquals(savedOrg.uuid, userCaptor.firstValue.organizationId)
    }

    @Test
    fun `register should throw exception when username already exists`() {
        val request = createValidRegisterRequest()
        `when`(passwordEncoder.encode(any())).thenReturn("encodedPassword")
        `when`(organizationRepository.save(any<Organizations>()))
            .thenReturn(createMockOrganization())
        `when`(userRepository.save(any<User>()))
            .thenThrow(DuplicateKeyException("E11000 duplicate key error collection: synectix.users index: username"))

        val exception = assertThrows<IllegalArgumentException> {
            authService.register(request)
        }
        assertEquals("Username already exists", exception.message)
    }

    @Test
    fun `register should throw exception when email already exists`() {
        val request = createValidRegisterRequest()
        `when`(passwordEncoder.encode(any())).thenReturn("encodedPassword")
        `when`(organizationRepository.save(any<Organizations>()))
            .thenReturn(createMockOrganization())
        `when`(userRepository.save(any<User>()))
            .thenThrow(DuplicateKeyException("E11000 duplicate key error collection: synectix.users index: email"))

        val exception = assertThrows<IllegalArgumentException> {
            authService.register(request)
        }
        assertEquals("Email already exists", exception.message)
    }

    @Test
    fun `register should throw exception when organization slug already exists`() {
        val request = createValidRegisterRequest()
        `when`(organizationRepository.save(any<Organizations>()))
            .thenThrow(DuplicateKeyException("E11000 duplicate key error collection: synectix.organizations index: orgSlug"))

        val exception = assertThrows<IllegalArgumentException> {
            authService.register(request)
        }
        assertEquals("Organization slug already exists", exception.message)
    }

    @Test
    fun `register should throw exception when organization name already exists`() {
        val request = createValidRegisterRequest()
        `when`(organizationRepository.save(any<Organizations>()))
            .thenThrow(DuplicateKeyException("E11000 duplicate key error collection: synectix.organizations index: name"))

        val exception = assertThrows<IllegalArgumentException> {
            authService.register(request)
        }
        assertEquals("Organization name already exists", exception.message)
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
        assertEquals(encodedPassword, userCaptor.firstValue.passwordHash)
    }


    @Test
    fun `login should return auth response with valid credentials`() {
        val request = LoginRequest(username = "testuser", password = "password123")
        val user = User(
            uuid = "user-123",
            username = request.username,
            email = "test@example.com",
            firstName = "Test",
            lastName = "User",
            passwordHash = "encodedPassword",
            organizationId = "org-123",
            roles = listOf("USER", "ADMIN"),
        )
        val savedToken = createMockSessionToken()

        `when`(userRepository.findByUsername(request.username)).thenReturn(Optional.of(user))
        `when`(passwordEncoder.matches(request.password, user.passwordHash)).thenReturn(true)
        `when`(sessionTokenRepository.save(any<SessionToken>())).thenReturn(savedToken)
        `when`(refreshTokenRepository.save(any<RefreshToken>())).thenAnswer { it.arguments[0] }

        val result = authService.login(request)

        assertNotNull(result)
        assertEquals(user.username, result.username)
        assertEquals(user.roles, result.roles)
        assertNotNull(result.accessToken)
        assertNotNull(result.expiresAt)

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

        assertNotNull(result.accessToken)
        assertTrue(result.accessToken.isNotEmpty())
        assertNotNull(result.refreshToken)
        assertTrue(result.refreshToken.isNotEmpty())

        val expectedRefreshExpiry = LocalDateTime.now().plusDays(30)
        val actualRefreshExpiry = LocalDateTime.parse(result.refreshTokenExpiresAt)
        assertTrue(actualRefreshExpiry.isAfter(expectedRefreshExpiry.minusMinutes(1)))
        assertTrue(actualRefreshExpiry.isBefore(expectedRefreshExpiry.plusMinutes(1)))
    }

    @Test
    fun `login should throw exception with invalid username`() {
        val request = LoginRequest(username = "nonexistent", password = "password123")
        `when`(userRepository.findByUsername(request.username)).thenReturn(Optional.empty())

        val exception = assertThrows<IllegalArgumentException> {
            authService.login(request)
        }
        assertEquals("Invalid username or password", exception.message)
    }

    @Test
    fun `login should throw exception with invalid password`() {
        val request = LoginRequest(username = "testuser", password = "wrongpassword")
        val user = createMockUser()

        `when`(userRepository.findByUsername(request.username)).thenReturn(Optional.of(user))
        `when`(passwordEncoder.matches(request.password, user.passwordHash)).thenReturn(false)

        val exception = assertThrows<IllegalArgumentException> {
            authService.login(request)
        }
        assertEquals("Invalid username or password", exception.message)
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
        assertEquals(user.uuid, capturedToken.userId)

        val expectedExpiry = LocalDateTime.now().plusHours(24)
        val actualExpiry = capturedToken.expiryAt
        assertTrue(actualExpiry.isAfter(expectedExpiry.minusMinutes(1)))
        assertTrue(actualExpiry.isBefore(expectedExpiry.plusMinutes(1)))
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

        assertTrue(result1.accessToken != result2.accessToken)
        assertTrue(result1.accessToken.matches(Regex("^[A-Za-z0-9_-]+$")))
    }

    @Test
    fun `login should throw exception when user account is inactive`() {
        val request = LoginRequest(username = "testuser", password = "password123")
        val inactiveUser = createMockUser().copy(isActive = false)

        `when`(userRepository.findByUsername(request.username)).thenReturn(Optional.of(inactiveUser))

        val exception = assertThrows<IllegalArgumentException> {
            authService.login(request)
        }
        assertEquals("User account is inactive", exception.message)
    }


    @Test
    fun `refresh should return new token pair with valid refresh token`() {
        val oldRefreshTokenStr = "old-refresh-token"
        val user = createMockUser()
        val oldSessionToken = createMockSessionToken()
        val existingRefreshToken = RefreshToken(
            id = "rt-123",
            token = oldRefreshTokenStr,
            userId = user.uuid,
            sessionTokenId = oldSessionToken.id,
            expiryAt = LocalDateTime.now().plusDays(30),
        )

        `when`(refreshTokenRepository.findByToken(oldRefreshTokenStr)).thenReturn(Optional.of(existingRefreshToken))
        `when`(userRepository.findById(user.uuid)).thenReturn(Optional.of(user))
        `when`(sessionTokenRepository.save(any<SessionToken>())).thenAnswer { it.arguments[0] }
        `when`(refreshTokenRepository.save(any<RefreshToken>())).thenAnswer { it.arguments[0] }
        `when`(mongoTemplate.findAndRemove(any(), eq(RefreshToken::class.java))).thenReturn(existingRefreshToken)

        val result = authService.refresh(oldRefreshTokenStr)

        assertNotNull(result.accessToken)
        assertNotNull(result.refreshToken)
        assertTrue(result.accessToken != oldSessionToken.token)
        assertTrue(result.refreshToken != oldRefreshTokenStr)

        verify(sessionTokenRepository).deleteById(oldSessionToken.id)
    }

    @Test
    fun `refresh should throw exception with expired refresh token`() {
        val expiredTokenStr = "expired-refresh-token"
        val expiredRefreshToken = RefreshToken(
            token = expiredTokenStr,
            userId = "user-123",
            sessionTokenId = "session-123",
            expiryAt = LocalDateTime.now().minusHours(1),
        )

        `when`(refreshTokenRepository.findByToken(expiredTokenStr)).thenReturn(Optional.of(expiredRefreshToken))

        val exception = assertThrows<IllegalArgumentException> {
            authService.refresh(expiredTokenStr)
        }
        assertEquals("Invalid or expired refresh token", exception.message)
    }

    @Test
    fun `refresh should throw exception with invalid refresh token`() {
        val invalidTokenStr = "invalid-refresh-token"

        `when`(refreshTokenRepository.findByToken(invalidTokenStr)).thenReturn(Optional.empty())

        val exception = assertThrows<IllegalArgumentException> {
            authService.refresh(invalidTokenStr)
        }
        assertEquals("Invalid or expired refresh token", exception.message)
    }

    @Test
    fun `refresh should throw exception when user is inactive`() {
        val refreshTokenStr = "valid-refresh-token"
        val inactiveUser = createMockUser().copy(isActive = false)
        val existingRefreshToken = RefreshToken(
            token = refreshTokenStr,
            userId = inactiveUser.uuid,
            sessionTokenId = "session-123",
            expiryAt = LocalDateTime.now().plusDays(30),
        )

        `when`(refreshTokenRepository.findByToken(refreshTokenStr)).thenReturn(Optional.of(existingRefreshToken))
        `when`(userRepository.findById(inactiveUser.uuid)).thenReturn(Optional.of(inactiveUser))

        val exception = assertThrows<IllegalArgumentException> {
            authService.refresh(refreshTokenStr)
        }
        assertEquals("User account is inactive", exception.message)
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


    private fun createValidRegisterRequest() = RegisterRequest(
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

    private fun createMockOrganization() = Organizations(
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

    private fun createMockUser() = User(
        uuid = "user-123",
        username = "testuser",
        email = "test@example.com",
        firstName = "Test",
        lastName = "User",
        passwordHash = "encodedPassword",
        organizationId = "org-123",
    )

    private fun createMockSessionToken() = SessionToken(
        id = "token-123",
        token = "generated-token",
        userId = "user-123",
        expiryAt = LocalDateTime.now().plusHours(24),
    )
}
