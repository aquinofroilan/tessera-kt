package com.loom.synectix.controller

import com.loom.synectix.aspect.LoggingAspect
import com.loom.synectix.config.TestSecurityConfig
import com.loom.synectix.dto.AuthResponse
import com.loom.synectix.dto.LoginRequest
import com.loom.synectix.dto.RegisterRequest
import com.loom.synectix.dto.UserOrganizationResponse
import com.loom.synectix.exception.AuthenticationException
import com.loom.synectix.exception.BusinessRuleException
import com.loom.synectix.model.RoleAssignment
import com.loom.synectix.model.User
import com.loom.synectix.repository.OrganizationRepository
import com.loom.synectix.repository.PasswordResetTokenRepository
import com.loom.synectix.repository.RefreshTokenRepository
import com.loom.synectix.repository.SessionTokenRepository
import com.loom.synectix.repository.UserRepository
import com.loom.synectix.security.RolePermissionCache
import com.loom.synectix.security.SessionContext
import com.loom.synectix.security.SynectixPermissionEvaluator
import com.loom.synectix.service.ApiKeyService
import com.loom.synectix.service.AuthService
import com.loom.synectix.util.TokenHasher
import org.junit.jupiter.api.Test
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.time.ZoneOffset

@WebMvcTest(controllers = [AuthController::class])
@Import(LoggingAspect::class, TestSecurityConfig::class, SynectixPermissionEvaluator::class)
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

    @MockitoBean
    private lateinit var passwordResetTokenRepository: PasswordResetTokenRepository

    @MockitoBean
    private lateinit var tokenHasher: TokenHasher

    @MockitoBean
    private lateinit var rolePermissionCache: RolePermissionCache

    @MockitoBean
    private lateinit var apiKeyService: ApiKeyService

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
            ).andExpect(status().isCreated)
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
            .thenThrow(BusinessRuleException("Username already exists"))

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
            .thenThrow(BusinessRuleException("Email already exists"))

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
                roles = listOf("OWNER"),
                organizationId = "org-123",
                expiresAt = LocalDateTime.now(ZoneOffset.UTC).plusHours(24).toString(),
                refreshTokenExpiresAt = LocalDateTime.now(ZoneOffset.UTC).plusDays(30).toString(),
            )

        `when`(authService.login(any<LoginRequest>(), anyOrNull(), anyOrNull())).thenReturn(authResponse)

        mockMvc
            .perform(
                post("/auth/signin")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").value(authResponse.accessToken))
            .andExpect(jsonPath("$.username").value(authResponse.username))
            .andExpect(jsonPath("$.roles[0]").value("OWNER"))
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

        `when`(authService.login(any<LoginRequest>(), anyOrNull(), anyOrNull()))
            .thenThrow(AuthenticationException("Invalid username or password"))

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

        `when`(authService.login(any<LoginRequest>(), anyOrNull(), anyOrNull()))
            .thenThrow(AuthenticationException("User account is inactive"))

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
                roles = listOf("OWNER"),
                organizationId = "org-123",
                expiresAt = LocalDateTime.now(ZoneOffset.UTC).plusHours(24).toString(),
                refreshTokenExpiresAt = LocalDateTime.now(ZoneOffset.UTC).plusDays(30).toString(),
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
            .thenThrow(AuthenticationException("Invalid or expired refresh token"))

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

    @Test
    fun `POST forgot-password should return 200 regardless of email existence`() {
        val requestJson =
            """
            {
                "email": "unknown@example.com"
            }
            """.trimIndent()

        `when`(authService.forgotPassword(any<String>())).thenReturn(null)

        mockMvc
            .perform(
                post("/auth/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.message").exists())
    }

    @Test
    fun `POST forgot-password should return 400 when email is blank`() {
        val requestJson =
            """
            {
                "email": ""
            }
            """.trimIndent()

        mockMvc
            .perform(
                post("/auth/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `POST forgot-password should throttle repeated requests for same email`() {
        val requestJson =
            """
            {
                "email": "throttle@example.com"
            }
            """.trimIndent()

        `when`(authService.forgotPassword(any<String>())).thenReturn("reset-token")

        mockMvc
            .perform(
                post("/auth/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson),
            ).andExpect(status().isOk)

        mockMvc
            .perform(
                post("/auth/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson),
            ).andExpect(status().isOk)

        verify(authService, times(1)).forgotPassword(any<String>())
    }

    @Test
    fun `POST reset-password should return 200 with valid token`() {
        val requestJson =
            """
            {
                "token": "valid-reset-token",
                "newPassword": "NewSecurePass123!"
            }
            """.trimIndent()

        mockMvc
            .perform(
                post("/auth/reset-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Password has been reset successfully"))
    }

    @Test
    fun `POST reset-password should return 400 with invalid token`() {
        val requestJson =
            """
            {
                "token": "invalid-token",
                "newPassword": "NewSecurePass123!"
            }
            """.trimIndent()

        `when`(authService.resetPassword(any<String>(), any<String>()))
            .thenThrow(BusinessRuleException("Invalid or expired reset token"))

        mockMvc
            .perform(
                post("/auth/reset-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Invalid or expired reset token"))
    }

    @Test
    fun `POST reset-password should return 400 when password is too short`() {
        val requestJson =
            """
            {
                "token": "valid-token",
                "newPassword": "short"
            }
            """.trimIndent()

        mockMvc
            .perform(
                post("/auth/reset-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `GET organizations should return org list for authenticated user`() {
        val user =
            User(
                uuid = "user-123",
                username = "testuser",
                email = "test@example.com",
                firstName = "Test",
                lastName = "User",
                passwordHash = "encoded",
                organizationId = "org-123",
                roleAssignments = listOf(RoleAssignment("OWNER", "org-123")),
            )
        val session = SessionContext(sessionId = "session-123", organizationId = "org-123")
        setupAuth(user, session)

        val orgs =
            listOf(
                UserOrganizationResponse(
                    organizationId = "org-123",
                    name = "Test Org",
                    orgSlug = "test-org",
                    roles = listOf("OWNER"),
                    isCurrent = true,
                    isActive = true,
                ),
            )
        `when`(authService.listUserOrganizations(any(), any())).thenReturn(orgs)

        mockMvc
            .perform(get("/auth/organizations"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].organizationId").value("org-123"))
            .andExpect(jsonPath("$[0].current").value(true))
            .andExpect(jsonPath("$[0].active").value(true))
    }

    @Test
    fun `POST organizations switch should return new token pair`() {
        val user =
            User(
                uuid = "user-123",
                username = "testuser",
                email = "test@example.com",
                firstName = "Test",
                lastName = "User",
                passwordHash = "encoded",
                organizationId = "org-123",
                roleAssignments =
                    listOf(
                        RoleAssignment("OWNER", "org-123"),
                        RoleAssignment("MEMBER", "org-456"),
                    ),
            )
        val session = SessionContext(sessionId = "session-123", organizationId = "org-123")
        setupAuth(user, session)

        val switchResponse =
            AuthResponse(
                accessToken = "new-token",
                refreshToken = "new-refresh",
                username = "testuser",
                roles = listOf("MEMBER"),
                organizationId = "org-456",
                expiresAt = LocalDateTime.now(ZoneOffset.UTC).plusHours(24).toString(),
                refreshTokenExpiresAt = LocalDateTime.now(ZoneOffset.UTC).plusDays(30).toString(),
            )
        `when`(authService.switchOrganization(any(), any(), anyOrNull(), anyOrNull())).thenReturn(switchResponse)

        mockMvc
            .perform(
                post("/auth/organizations/switch")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"organizationId": "org-456"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.organizationId").value("org-456"))
            .andExpect(jsonPath("$.roles[0]").value("MEMBER"))
            .andExpect(jsonPath("$.accessToken").value("new-token"))
    }

    @Test
    fun `POST organizations switch should return 400 for unauthorized org`() {
        val user =
            User(
                uuid = "user-123",
                username = "testuser",
                email = "test@example.com",
                firstName = "Test",
                lastName = "User",
                passwordHash = "encoded",
                organizationId = "org-123",
                roleAssignments = listOf(RoleAssignment("OWNER", "org-123")),
            )
        val session = SessionContext(sessionId = "session-123", organizationId = "org-123")
        setupAuth(user, session)

        `when`(authService.switchOrganization(any(), any(), anyOrNull(), anyOrNull()))
            .thenThrow(BusinessRuleException("You do not have access to this organization"))

        mockMvc
            .perform(
                post("/auth/organizations/switch")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"organizationId": "org-999"}"""),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("You do not have access to this organization"))
    }

    private fun setupAuth(
        user: User,
        session: SessionContext,
    ) {
        val authorities = user.roleAssignments.map { SimpleGrantedAuthority("ROLE_${it.role}") }
        val authentication = UsernamePasswordAuthenticationToken(user, null, authorities)
        authentication.details = session
        SecurityContextHolder.getContext().authentication = authentication
    }
}
