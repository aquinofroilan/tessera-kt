package com.froilan.synectix.controller

import com.froilan.synectix.aspect.LoggingAspect
import com.froilan.synectix.config.TestSecurityConfig
import com.froilan.synectix.exception.ResourceNotFoundException
import com.froilan.synectix.model.RoleAssignment
import com.froilan.synectix.model.SessionToken
import com.froilan.synectix.model.User
import com.froilan.synectix.repository.OrganizationRepository
import com.froilan.synectix.repository.PasswordResetTokenRepository
import com.froilan.synectix.repository.RefreshTokenRepository
import com.froilan.synectix.repository.SessionTokenRepository
import com.froilan.synectix.repository.UserRepository
import com.froilan.synectix.security.AuthenticationContext
import com.froilan.synectix.security.RolePermissionCache
import com.froilan.synectix.security.SynectixPermissionEvaluator
import com.froilan.synectix.service.ApiKeyService
import com.froilan.synectix.service.AuthService
import com.froilan.synectix.util.TokenHasher
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.time.ZoneOffset

@WebMvcTest(controllers = [SessionController::class])
@Import(LoggingAspect::class, TestSecurityConfig::class, SynectixPermissionEvaluator::class)
@ActiveProfiles("test")
class SessionControllerTest {
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

    @MockitoBean
    private lateinit var authenticationContext: AuthenticationContext

    private val testUser =
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

    private val currentToken = "current-bearer-token"

    @BeforeEach
    fun setup() {
        setupAuthWithPermissions("session:read", "session:delete")
    }

    private fun setupAuthWithPermissions(vararg permissions: String) {
        val roleAuthorities = testUser.roleAssignments.map { SimpleGrantedAuthority("ROLE_${it.role}") }
        val permissionAuthorities = permissions.map { SimpleGrantedAuthority(it) }
        val authentication = UsernamePasswordAuthenticationToken(testUser, null, roleAuthorities + permissionAuthorities)
        SecurityContextHolder.getContext().authentication = authentication
    }

    @Test
    fun `GET sessions should return list with current session indicator`() {
        val sessions =
            listOf(
                SessionToken(
                    id = "s1",
                    token = currentToken,
                    userId = testUser.uuid,
                    expiryAt = LocalDateTime.now(ZoneOffset.UTC).plusHours(12),
                    ipAddress = "127.0.0.1",
                    userAgent = "Mozilla/5.0",
                ),
                SessionToken(
                    id = "s2",
                    token = "other-token",
                    userId = testUser.uuid,
                    expiryAt = LocalDateTime.now(ZoneOffset.UTC).plusHours(6),
                    ipAddress = "192.168.1.1",
                    userAgent = "Chrome",
                ),
            )
        `when`(authService.listSessions(testUser.uuid)).thenReturn(sessions)

        mockMvc
            .perform(
                get("/auth/sessions")
                    .header("Authorization", "Bearer $currentToken"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value("s1"))
            .andExpect(jsonPath("$[0].current").value(true))
            .andExpect(jsonPath("$[0].ipAddress").value("127.0.0.1"))
            .andExpect(jsonPath("$[1].id").value("s2"))
            .andExpect(jsonPath("$[1].current").value(false))
    }

    @Test
    fun `GET sessions should return empty list when no sessions`() {
        `when`(authService.listSessions(testUser.uuid)).thenReturn(emptyList())

        mockMvc
            .perform(
                get("/auth/sessions")
                    .header("Authorization", "Bearer $currentToken"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `DELETE session by id should return 200 on success`() {
        mockMvc
            .perform(
                delete("/auth/sessions/s2")
                    .header("Authorization", "Bearer $currentToken"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Session revoked"))
    }

    @Test
    fun `DELETE session by id should return 404 for non-owned session`() {
        `when`(authService.revokeSession(any(), any(), any()))
            .thenThrow(ResourceNotFoundException("Session not found"))

        mockMvc
            .perform(
                delete("/auth/sessions/non-existent")
                    .header("Authorization", "Bearer $currentToken"),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error").value("Session not found"))
    }

    @Test
    fun `DELETE session by id should return 400 when revoking the current session`() {
        `when`(authService.revokeSession(any(), any(), any()))
            .thenThrow(IllegalStateException("Cannot revoke the current session"))

        mockMvc
            .perform(
                delete("/auth/sessions/s1")
                    .header("Authorization", "Bearer $currentToken"),
            ).andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.error").value("Cannot revoke the current session"))
    }

    @Test
    fun `DELETE sessions should revoke others and return 200`() {
        mockMvc
            .perform(
                delete("/auth/sessions")
                    .header("Authorization", "Bearer $currentToken"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("All other sessions revoked"))
    }

    @Test
    fun `GET sessions should return 403 without session read permission`() {
        setupAuthWithPermissions()

        mockMvc
            .perform(
                get("/auth/sessions")
                    .header("Authorization", "Bearer $currentToken"),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `DELETE session should return 403 without session delete permission`() {
        setupAuthWithPermissions("session:read")

        mockMvc
            .perform(
                delete("/auth/sessions/s2")
                    .header("Authorization", "Bearer $currentToken"),
            ).andExpect(status().isForbidden)
    }
}
