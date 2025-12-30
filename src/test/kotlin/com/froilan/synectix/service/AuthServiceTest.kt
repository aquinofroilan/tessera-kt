package com.froilan.synectix.service

import com.froilan.synectix.dto.LoginRequest
import com.froilan.synectix.dto.RegisterRequest
import com.froilan.synectix.model.Organizations
import com.froilan.synectix.model.SessionToken
import com.froilan.synectix.model.User
import com.froilan.synectix.repository.OrganizationRepository
import com.froilan.synectix.repository.SessionTokenRepository
import com.froilan.synectix.repository.UserRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.springframework.dao.DuplicateKeyException
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AuthServiceTest {

    private lateinit var authService: AuthService
    private lateinit var userRepository: UserRepository
    private lateinit var organizationRepository: OrganizationRepository
    private lateinit var sessionTokenRepository: SessionTokenRepository
    private lateinit var passwordEncoder: PasswordEncoder

    @BeforeEach
    fun setup() {
        userRepository = mock(UserRepository::class.java)
        organizationRepository = mock(OrganizationRepository::class.java)
        sessionTokenRepository = mock(SessionTokenRepository::class.java)
        passwordEncoder = mock(PasswordEncoder::class.java)

        authService = AuthService(
            userRepository,
            organizationRepository,
            sessionTokenRepository,
            passwordEncoder
        )
    }

    // ========== Registration Tests ==========

    @Test
    fun `register should create organization and user successfully`() {
        // Given
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
            tradeName = request.orgTradeName
        )
        val savedUser = User(
            uuid = "user-123",
            username = request.username,
            email = request.email,
            firstName = request.firstName,
            lastName = request.lastName,
            passwordHash = encodedPassword,
            organizationId = savedOrg.uuid
        )

        `when`(passwordEncoder.encode(request.password)).thenReturn(encodedPassword)
        `when`(organizationRepository.save(any<Organizations>())).thenReturn(savedOrg)
        `when`(userRepository.save(any<User>())).thenReturn(savedUser)

        // When
        val result = authService.register(request)

        // Then
        assertNotNull(result)
        assertEquals(request.username, result.username)
        assertEquals(request.email, result.email)
        assertEquals(savedOrg.uuid, result.organizationId)

        // Verify organization was saved
        verify(organizationRepository, times(1)).save(any<Organizations>())

        // Verify user was saved with correct data
        val userCaptor = argumentCaptor<User>()
        verify(userRepository, times(1)).save(userCaptor.capture())
        assertEquals(encodedPassword, userCaptor.firstValue.passwordHash)
        assertEquals(savedOrg.uuid, userCaptor.firstValue.organizationId)
    }

    @Test
    fun `register should throw exception when username already exists`() {
        // Given
        val request = createValidRegisterRequest()
        `when`(passwordEncoder.encode(any())).thenReturn("encodedPassword")
        `when`(organizationRepository.save(any<Organizations>()))
            .thenReturn(createMockOrganization())
        `when`(userRepository.save(any<User>()))
            .thenThrow(DuplicateKeyException("E11000 duplicate key error collection: synectix.users index: username"))

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            authService.register(request)
        }
        assertEquals("Username already exists", exception.message)
    }

    @Test
    fun `register should throw exception when email already exists`() {
        // Given
        val request = createValidRegisterRequest()
        `when`(passwordEncoder.encode(any())).thenReturn("encodedPassword")
        `when`(organizationRepository.save(any<Organizations>()))
            .thenReturn(createMockOrganization())
        `when`(userRepository.save(any<User>()))
            .thenThrow(DuplicateKeyException("E11000 duplicate key error collection: synectix.users index: email"))

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            authService.register(request)
        }
        assertEquals("Email already exists", exception.message)
    }

    @Test
    fun `register should throw exception when organization slug already exists`() {
        // Given
        val request = createValidRegisterRequest()
        `when`(organizationRepository.save(any<Organizations>()))
            .thenThrow(DuplicateKeyException("E11000 duplicate key error collection: synectix.organizations index: orgSlug"))

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            authService.register(request)
        }
        assertEquals("Organization slug already exists", exception.message)
    }

    @Test
    fun `register should throw exception when organization name already exists`() {
        // Given
        val request = createValidRegisterRequest()
        `when`(organizationRepository.save(any<Organizations>()))
            .thenThrow(DuplicateKeyException("E11000 duplicate key error collection: synectix.organizations index: name"))

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            authService.register(request)
        }
        assertEquals("Organization name already exists", exception.message)
    }

    @Test
    fun `register should encode password before saving`() {
        // Given
        val request = createValidRegisterRequest()
        val encodedPassword = "super-secure-encoded-password"

        `when`(passwordEncoder.encode(request.password)).thenReturn(encodedPassword)
        `when`(organizationRepository.save(any<Organizations>())).thenReturn(createMockOrganization())
        `when`(userRepository.save(any<User>())).thenReturn(createMockUser())

        // When
        authService.register(request)

        // Then
        verify(passwordEncoder, times(1)).encode(request.password)
        val userCaptor = argumentCaptor<User>()
        verify(userRepository).save(userCaptor.capture())
        assertEquals(encodedPassword, userCaptor.firstValue.passwordHash)
    }

    // ========== Login Tests ==========

    @Test
    fun `login should return auth response with valid credentials`() {
        // Given
        val request = LoginRequest(username = "testuser", password = "password123")
        val user = User(
            uuid = "user-123",
            username = request.username,
            email = "test@example.com",
            firstName = "Test",
            lastName = "User",
            passwordHash = "encodedPassword",
            organizationId = "org-123",
            roles = listOf("USER", "ADMIN")
        )
        val savedToken = SessionToken(
            id = "token-123",
            token = "generated-token",
            userId = user.uuid,
            expiryAt = LocalDateTime.now().plusHours(24)
        )

        `when`(userRepository.findByUsername(request.username)).thenReturn(Optional.of(user))
        `when`(passwordEncoder.matches(request.password, user.passwordHash)).thenReturn(true)
        `when`(sessionTokenRepository.save(any<SessionToken>())).thenReturn(savedToken)

        // When
        val result = authService.login(request)

        // Then
        assertNotNull(result)
        assertEquals(user.username, result.username)
        assertEquals(user.roles, result.roles)
        assertNotNull(result.token)
        assertNotNull(result.expiresAt)

        verify(sessionTokenRepository, times(1)).save(any<SessionToken>())
    }

    @Test
    fun `login should throw exception with invalid username`() {
        // Given
        val request = LoginRequest(username = "nonexistent", password = "password123")
        `when`(userRepository.findByUsername(request.username)).thenReturn(Optional.empty())

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            authService.login(request)
        }
        assertEquals("Invalid username or password", exception.message)
    }

    @Test
    fun `login should throw exception with invalid password`() {
        // Given
        val request = LoginRequest(username = "testuser", password = "wrongpassword")
        val user = createMockUser()

        `when`(userRepository.findByUsername(request.username)).thenReturn(Optional.of(user))
        `when`(passwordEncoder.matches(request.password, user.passwordHash)).thenReturn(false)

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            authService.login(request)
        }
        assertEquals("Invalid username or password", exception.message)
    }

    @Test
    fun `login should create session token with 24 hour expiry`() {
        // Given
        val request = LoginRequest(username = "testuser", password = "password123")
        val user = createMockUser()

        `when`(userRepository.findByUsername(request.username)).thenReturn(Optional.of(user))
        `when`(passwordEncoder.matches(request.password, user.passwordHash)).thenReturn(true)
        `when`(sessionTokenRepository.save(any<SessionToken>())).thenReturn(createMockSessionToken())

        // When
        authService.login(request)

        // Then
        val tokenCaptor = argumentCaptor<SessionToken>()
        verify(sessionTokenRepository).save(tokenCaptor.capture())

        val capturedToken = tokenCaptor.firstValue
        assertEquals(user.uuid, capturedToken.userId)

        // Verify expiry is approximately 24 hours from now (with 1 minute tolerance)
        val expectedExpiry = LocalDateTime.now().plusHours(24)
        val actualExpiry = capturedToken.expiryAt
        assertTrue(actualExpiry.isAfter(expectedExpiry.minusMinutes(1)))
        assertTrue(actualExpiry.isBefore(expectedExpiry.plusMinutes(1)))
    }

    @Test
    fun `login should generate secure random token`() {
        // Given
        val request = LoginRequest(username = "testuser", password = "password123")
        val user = createMockUser()

        `when`(userRepository.findByUsername(request.username)).thenReturn(Optional.of(user))
        `when`(passwordEncoder.matches(request.password, user.passwordHash)).thenReturn(true)
        `when`(sessionTokenRepository.save(any<SessionToken>())).thenAnswer { it.arguments[0] }

        // When
        val result1 = authService.login(request)
        val result2 = authService.login(request)

        // Then
        // Tokens should be different (randomly generated)
        assertTrue(result1.token != result2.token)
        // Token should be base64 URL-safe (no padding)
        assertTrue(result1.token.matches(Regex("^[A-Za-z0-9_-]+$")))
    }

    // ========== Logout Tests ==========

    @Test
    fun `logout should delete session token`() {
        // Given
        val token = "test-token-123"

        // When
        authService.logout(token)

        // Then
        verify(sessionTokenRepository, times(1)).deleteByToken(token)
    }

    // ========== Helper Methods ==========

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
        orgTradeName = "Test Org"
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
        tradeName = "Test Org"
    )

    private fun createMockUser() = User(
        uuid = "user-123",
        username = "testuser",
        email = "test@example.com",
        firstName = "Test",
        lastName = "User",
        passwordHash = "encodedPassword",
        organizationId = "org-123"
    )

    private fun createMockSessionToken() = SessionToken(
        id = "token-123",
        token = "generated-token",
        userId = "user-123",
        expiryAt = LocalDateTime.now().plusHours(24)
    )
}
