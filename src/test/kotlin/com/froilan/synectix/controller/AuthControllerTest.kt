package com.froilan.synectix.controller

import com.froilan.synectix.aspect.LoggingAspect
import com.froilan.synectix.dto.LoginRequest
import com.froilan.synectix.dto.RegisterRequest
import com.froilan.synectix.model.User
import com.froilan.synectix.service.AuthService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDateTime

@WebMvcTest(controllers = [AuthController::class])
@Import(LoggingAspect::class, com.froilan.synectix.config.TestSecurityConfig::class)
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var authService: AuthService

    @MockitoBean
    private lateinit var sessionTokenRepository: com.froilan.synectix.repository.SessionTokenRepository

    @MockitoBean
    private lateinit var userRepository: com.froilan.synectix.repository.UserRepository

    @MockitoBean
    private lateinit var tokenAuthenticationFilter: com.froilan.synectix.config.TokenAuthenticationFilter

    @MockitoBean
    private lateinit var organizationRepository: com.froilan.synectix.repository.OrganizationRepository

    @MockitoBean
    private lateinit var passwordEncoder: org.springframework.security.crypto.password.PasswordEncoder

    // ========== Signup Tests ==========

    @Test
    fun `POST signup should return 201 when registration is successful`() {
        // Given
        val requestJson = """
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

        val savedUser = User(
            uuid = "user-123",
            username = "newuser",
            email = "newuser@example.com",
            firstName = "New",
            lastName = "User",
            passwordHash = "encodedPassword",
            organizationId = "org-123"
        )

        `when`(authService.register(any<RegisterRequest>())).thenReturn(savedUser)

        // When & Then
        mockMvc.perform(
            post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            .andDo { result ->
                println("Response status: ${result.response.status}")
                println("Response body: ${result.response.contentAsString}")
            }
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.message").value("User registered successfully"))
            .andExpect(jsonPath("$.userId").value(savedUser.uuid))
    }

    @Test
    
    fun `POST signup should return 400 when username already exists`() {
        // Given
        val requestJson = """
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

        // When & Then
        mockMvc.perform(
            post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Username already exists"))
    }

    @Test
    
    fun `POST signup should return 400 when email already exists`() {
        // Given
        val requestJson = """
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

        // When & Then
        mockMvc.perform(
            post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Email already exists"))
    }

    // ========== Signin Tests ==========

    @Test
    
    fun `POST signin should return 200 with token when login is successful`() {
        // Given
        val requestJson = """
            {
                "username": "testuser",
                "password": "password123"
            }
        """.trimIndent()

        val authResponse = com.froilan.synectix.dto.AuthResponse(
            token = "generated-token-123",
            username = "testuser",
            roles = listOf("USER"),
            expiresAt = LocalDateTime.now().plusHours(24).toString()
        )

        `when`(authService.login(any<LoginRequest>())).thenReturn(authResponse)

        // When & Then
        mockMvc.perform(
            post("/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token").value(authResponse.token))
            .andExpect(jsonPath("$.username").value(authResponse.username))
            .andExpect(jsonPath("$.roles[0]").value("USER"))
            .andExpect(jsonPath("$.expiresAt").exists())
    }

    @Test
    
    fun `POST signin should return 400 when credentials are invalid`() {
        // Given
        val requestJson = """
            {
                "username": "testuser",
                "password": "wrongpassword"
            }
        """.trimIndent()

        `when`(authService.login(any<LoginRequest>()))
            .thenThrow(IllegalArgumentException("Invalid username or password"))

        // When & Then
        mockMvc.perform(
            post("/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Invalid username or password"))
    }
}
