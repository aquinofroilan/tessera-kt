package com.froilan.synectix.controller

import com.froilan.synectix.aspect.LoggingAspect
import com.froilan.synectix.config.TestSecurityConfig
import com.froilan.synectix.dto.AuthResponse
import com.froilan.synectix.dto.LoginRequest
import com.froilan.synectix.dto.RegisterRequest
import com.froilan.synectix.model.User
import com.froilan.synectix.repository.OrganizationRepository
import com.froilan.synectix.repository.RefreshTokenRepository
import com.froilan.synectix.repository.SessionTokenRepository
import com.froilan.synectix.repository.UserRepository
import com.froilan.synectix.service.AuthService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@WebMvcTest(controllers = [AuthController::class])
@Import(LoggingAspect::class, TestSecurityConfig::class)
@ActiveProfiles("test")
class AuthControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var authService: AuthService

    @MockitoBean
    private lateinit var sessionTokenRepository: SessionTokenRepository

    @MockitoBean
    private lateinit var userRepository: UserRepository

    @MockitoBean
    private lateinit var organizationRepository: OrganizationRepository

    @MockitoBean
    private lateinit var passwordEncoder: PasswordEncoder

    @MockitoBean
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    @Test
    fun `POST signup should return 201 when registration is successful`() {
        val requestJson =
            """
            {
                "username": "newuser",
                "password": "SecurePass123!",
                "email": "newuser@example.com",
                "firstName": "New",
                "lastName": "User",
                "orgName": "New Organization",
                "orgSlug": "new-org",
                "orgDescription": "A new organization",
                "orgBaseCurrency": "USD",
                "orgFiscalYearStart": "2024-01-01T00:00:00",
                "orgTimezone": "UTC",
                "orgLegalName": "New Organization LLC",
                "orgTradeName": "New Org"
            }
            """.trimIndent()

        val savedUser =
            User(
                uuid = "user-123",
                username = "newuser",
                email = "newuser@example.com",
                firstName = "New",
                lastName = "User",
                passwordHash = "encodedPassword",
                organizationId = "org-123",
            )

        `when`(authService.register(any<RegisterRequest>())).thenReturn(savedUser)

        mockMvc
            .perform(
                post("/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson),
            ).andDo { result ->
                println("Response status: ${result.response.status}")
                println("Response body: ${result.response.contentAsString}")
            }.andExpect(status().isCreated)
            .andExpect(jsonPath("$.message").value("User registered successfully"))
            .andExpect(jsonPath("$.userId").value(savedUser.uuid))
    }

    @Test
    fun `POST signup should return 400 when username already exists`() {
        val requestJson =
            """
            {
                "username": "existinguser",
                "password": "SecurePass123!",
                "email": "new@example.com",
                "firstName": "Test",
                "lastName": "User",
                "orgName": "Test Organization",
                "orgSlug": "test-org",
                "orgBaseCurrency": "USD",
                "orgFiscalYearStart": "2024-01-01T00:00:00",
                "orgTimezone": "UTC",
                "orgLegalName": "Test Organization LLC",
                "orgTradeName": "Test Org"
            }
            """.trimIndent()

        `when`(authService.register(any<RegisterRequest>()))
            .thenThrow(IllegalArgumentException("Username already exists"))

        mockMvc
            .perform(
                post("/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Username already exists"))
    }

    @Test
    fun `POST signup should return 400 when email already exists`() {
        val requestJson =
            """
            {
                "username": "newuser",
                "password": "SecurePass123!",
                "email": "existing@example.com",
                "firstName": "Test",
                "lastName": "User",
                "orgName": "Test Organization",
                "orgSlug": "test-org",
                "orgBaseCurrency": "USD",
                "orgFiscalYearStart": "2024-01-01T00:00:00",
                "orgTimezone": "UTC",
                "orgLegalName": "Test Organization LLC",
                "orgTradeName": "Test Org"
            }
            """.trimIndent()

        `when`(authService.register(any<RegisterRequest>()))
            .thenThrow(IllegalArgumentException("Email already exists"))

        mockMvc
            .perform(
                post("/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Email already exists"))
    }

    @Test
    fun `POST signup should return 400 when username is blank`() {
        val requestJson =
            """
            {
                "username": "",
                "password": "SecurePass123!",
                "email": "test@example.com",
                "firstName": "Test",
                "lastName": "User",
                "orgName": "Test Organization",
                "orgSlug": "test-org",
                "orgBaseCurrency": "USD",
                "orgFiscalYearStart": "2024-01-01T00:00:00",
                "orgTimezone": "UTC",
                "orgLegalName": "Test Organization LLC",
                "orgTradeName": "Test Org"
            }
            """.trimIndent()

        mockMvc
            .perform(
                post("/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `POST signup should return 400 when password is too short`() {
        val requestJson =
            """
            {
                "username": "testuser",
                "password": "short",
                "email": "test@example.com",
                "firstName": "Test",
                "lastName": "User",
                "orgName": "Test Organization",
                "orgSlug": "test-org",
                "orgBaseCurrency": "USD",
                "orgFiscalYearStart": "2024-01-01T00:00:00",
                "orgTimezone": "UTC",
                "orgLegalName": "Test Organization LLC",
                "orgTradeName": "Test Org"
            }
            """.trimIndent()

        mockMvc
            .perform(
                post("/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `POST signup should return 400 when email format is invalid`() {
        val requestJson =
            """
            {
                "username": "testuser",
                "password": "SecurePass123!",
                "email": "invalid-email",
                "firstName": "Test",
                "lastName": "User",
                "orgName": "Test Organization",
                "orgSlug": "test-org",
                "orgBaseCurrency": "USD",
                "orgFiscalYearStart": "2024-01-01T00:00:00",
                "orgTimezone": "UTC",
                "orgLegalName": "Test Organization LLC",
                "orgTradeName": "Test Org"
            }
            """.trimIndent()

        mockMvc
            .perform(
                post("/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `POST signin should return 200 with tokens when login is successful`() {
        val requestJson =
            """
            {
                "username": "testuser",
                "password": "password123"
            }
            """.trimIndent()

        val authResponse =
            AuthResponse(
                accessToken = "generated-token-123",
                refreshToken = "generated-refresh-token-123",
                username = "testuser",
                roles = listOf("USER"),
                expiresAt = LocalDateTime.now().plusHours(24).toString(),
                refreshTokenExpiresAt = LocalDateTime.now().plusDays(30).toString(),
            )

        `when`(authService.login(any<LoginRequest>())).thenReturn(authResponse)

        mockMvc
            .perform(
                post("/auth/signin")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").value(authResponse.accessToken))
            .andExpect(jsonPath("$.username").value(authResponse.username))
            .andExpect(jsonPath("$.roles[0]").value("USER"))
            .andExpect(jsonPath("$.expiresAt").exists())
    }

    @Test
    fun `POST signin should return 401 when credentials are invalid`() {
        val requestJson =
            """
            {
                "username": "testuser",
                "password": "wrongpassword"
            }
            """.trimIndent()

        `when`(authService.login(any<LoginRequest>()))
            .thenThrow(IllegalArgumentException("Invalid username or password"))

        mockMvc
            .perform(
                post("/auth/signin")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson),
            ).andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error").value("Invalid username or password"))
    }

    @Test
    fun `POST signin should return 400 when username is blank`() {
        val requestJson =
            """
            {
                "username": "",
                "password": "password123"
            }
            """.trimIndent()

        mockMvc
            .perform(
                post("/auth/signin")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `POST signin should return 400 when password is blank`() {
        val requestJson =
            """
            {
                "username": "testuser",
                "password": ""
            }
            """.trimIndent()

        mockMvc
            .perform(
                post("/auth/signin")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `POST signin should return 401 when user account is inactive`() {
        val requestJson =
            """
            {
                "username": "inactiveuser",
                "password": "password123"
            }
            """.trimIndent()

        `when`(authService.login(any<LoginRequest>()))
            .thenThrow(IllegalArgumentException("User account is inactive"))

        mockMvc
            .perform(
                post("/auth/signin")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson),
            ).andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error").value("User account is inactive"))
    }

    @Test
    fun `POST refresh should return 200 with new tokens when refresh token is valid`() {
        val requestJson =
            """
            {
                "refreshToken": "valid-refresh-token-123"
            }
            """.trimIndent()

        val authResponse =
            AuthResponse(
                accessToken = "new-access-token-456",
                refreshToken = "new-refresh-token-789",
                username = "testuser",
                roles = listOf("USER"),
                expiresAt = LocalDateTime.now().plusHours(24).toString(),
                refreshTokenExpiresAt = LocalDateTime.now().plusDays(30).toString(),
            )

        `when`(authService.refresh(any<String>())).thenReturn(authResponse)

        mockMvc
            .perform(
                post("/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists())
    }

    @Test
    fun `POST refresh should return 401 when refresh token is invalid`() {
        val requestJson =
            """
            {
                "refreshToken": "invalid-refresh-token"
            }
            """.trimIndent()

        `when`(authService.refresh(any<String>()))
            .thenThrow(IllegalArgumentException("Invalid or expired refresh token"))

        mockMvc
            .perform(
                post("/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson),
            ).andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error").value("Invalid or expired refresh token"))
    }

    @Test
    fun `POST refresh should return 400 when refresh token field is blank`() {
        val requestJson =
            """
            {
                "refreshToken": ""
            }
            """.trimIndent()

        mockMvc
            .perform(
                post("/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `POST logout should return 200 when token is valid`() {
        val token = "valid-token-123"
        val authHeader = "Bearer $token"

        mockMvc
            .perform(
                post("/auth/logout")
                    .header("Authorization", authHeader),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Logged out successfully"))
    }

    @Test
    fun `POST logout should return 200 when no authorization header is provided`() {
        mockMvc
            .perform(
                post("/auth/logout"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Logged out successfully"))
    }

    @Test
    fun `POST logout should return 200 when authorization header is empty`() {
        mockMvc
            .perform(
                post("/auth/logout")
                    .header("Authorization", ""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Logged out successfully"))
    }

    @Test
    fun `POST logout should return 200 when authorization header has invalid format`() {
        mockMvc
            .perform(
                post("/auth/logout")
                    .header("Authorization", "InvalidFormat token-123"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Logged out successfully"))
    }

    @Test
    fun `POST logout should invoke authService logout with valid token`() {
        val token = "valid-token-123"
        val authHeader = "Bearer $token"

        mockMvc.perform(
            post("/auth/logout")
                .header("Authorization", authHeader),
        )

        verify(authService, times(1)).logout(token)
    }
}
