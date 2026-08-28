package com.aquinofroilan.tessera.domain.auth.controller

import com.aquinofroilan.tessera.aspect.LoggingAspect
import com.aquinofroilan.tessera.config.TestSecurityConfig
import com.aquinofroilan.tessera.config.WebMvcConfig
import com.aquinofroilan.tessera.domain.auth.model.Invitation
import com.aquinofroilan.tessera.domain.auth.model.RoleAssignment
import com.aquinofroilan.tessera.domain.auth.model.User
import com.aquinofroilan.tessera.domain.auth.repository.InvitationRepository
import com.aquinofroilan.tessera.domain.auth.repository.PasswordResetTokenRepository
import com.aquinofroilan.tessera.domain.auth.repository.RefreshTokenRepository
import com.aquinofroilan.tessera.domain.auth.repository.SessionTokenRepository
import com.aquinofroilan.tessera.domain.auth.repository.UserRepository
import com.aquinofroilan.tessera.domain.auth.service.ApiKeyService
import com.aquinofroilan.tessera.domain.auth.service.InvitationService
import com.aquinofroilan.tessera.domain.organization.repository.OrganizationRepository
import com.aquinofroilan.tessera.domain.platform.dto.ValidateInvitationResponse
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.security.RolePermissionCache
import com.aquinofroilan.tessera.security.SessionContext
import com.aquinofroilan.tessera.security.TesseraPermissionEvaluator
import com.aquinofroilan.tessera.util.TokenHasher
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@WebMvcTest(controllers = [InvitationController::class])
@Import(WebMvcConfig::class, LoggingAspect::class, TestSecurityConfig::class, TesseraPermissionEvaluator::class)
@ActiveProfiles("test")
class InvitationControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var invitationService: InvitationService

    @MockitoBean
    private lateinit var sessionTokenRepository: SessionTokenRepository

    @MockitoBean
    private lateinit var userRepository: UserRepository

    @MockitoBean
    private lateinit var organizationRepository: OrganizationRepository

    @MockitoBean
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    @MockitoBean
    private lateinit var passwordResetTokenRepository: PasswordResetTokenRepository

    @MockitoBean
    private lateinit var invitationRepository: InvitationRepository

    @MockitoBean
    private lateinit var tokenHasher: TokenHasher

    @MockitoBean
    private lateinit var rolePermissionCache: RolePermissionCache

    @MockitoBean
    private lateinit var authenticationContext: AuthenticationContext

    @MockitoBean
    private lateinit var apiKeyService: ApiKeyService

    private val testUser =
        User(
            uuid = java.util.UUID.fromString("bc17c97c-3d89-7d43-b7e0-7ca0266eafa8"),
            username = "testuser",
            email = "test@example.com",
            firstName = "Test",
            lastName = "User",
            passwordHash = "encoded",
            organizationId = java.util.UUID.fromString("4abe9f6d-6df3-6e5c-953e-3695db9a5216"),
            roleAssignments = listOf(RoleAssignment("OWNER", java.util.UUID.fromString("4abe9f6d-6df3-6e5c-953e-3695db9a5216"))),
        )

    @BeforeEach
    fun setup() {
        setupAuthWithPermissions("invitation:read", "invitation:write")
    }

    private fun setupAuthWithPermissions(vararg permissions: String) {
        val roleAuthorities = testUser.roleAssignments.map { SimpleGrantedAuthority("ROLE_${it.role}") }
        val permissionAuthorities = permissions.map { SimpleGrantedAuthority(it) }
        val authentication = UsernamePasswordAuthenticationToken(testUser, null, roleAuthorities + permissionAuthorities)
        authentication.details =
            SessionContext(
                sessionId = java.util.UUID.fromString("79c5ca4c-8e48-a8f8-6ffc-5b3271a250aa"),
                organizationId = java.util.UUID.fromString("4abe9f6d-6df3-6e5c-953e-3695db9a5216"),
            )
        SecurityContextHolder.getContext().authentication = authentication
    }

    @Test
    fun `POST invitations should return 201 when invitation is created`() {
        `when`(invitationService.invite(any(), any(), any())).thenReturn("raw-token-123")

        mockMvc
            .perform(
                post("/api/v1/auth/invitations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"email": "newuser@example.com", "role": "MEMBER"}"""),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.message").value("Invitation created"))
            .andExpect(jsonPath("$.token").value("raw-token-123"))
    }

    @Test
    fun `POST invitations should return 400 when email is invalid`() {
        mockMvc
            .perform(
                post("/api/v1/auth/invitations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"email": "not-an-email", "role": "MEMBER"}"""),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `POST invitations should return 400 when role is blank`() {
        mockMvc
            .perform(
                post("/api/v1/auth/invitations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"email": "user@example.com", "role": ""}"""),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `POST invitations should return 400 when duplicate invitation exists`() {
        `when`(invitationService.invite(any(), any(), any()))
            .thenThrow(IllegalArgumentException("An invitation has already been sent to this email"))

        mockMvc
            .perform(
                post("/api/v1/auth/invitations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"email": "existing@example.com", "role": "MEMBER"}"""),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("An invitation has already been sent to this email"))
    }

    @Test
    fun `GET invitations should return 200 with invitation list`() {
        val invitations =
            listOf(
                Invitation(
                    id = java.util.UUID.fromString("a1dc238a-c774-5d3d-241b-f32dce1112ce"),
                    email = "user1@example.com",
                    organizationId = java.util.UUID.fromString("4abe9f6d-6df3-6e5c-953e-3695db9a5216"),
                    role = "MEMBER",
                    tokenHash = "hash1",
                    invitedBy = java.util.UUID.fromString("bc17c97c-3d89-7d43-b7e0-7ca0266eafa8"),
                    expiryAt = LocalDateTime.now(ZoneOffset.UTC).plusHours(72),
                ),
            )
        `when`(invitationService.listInvitations(java.util.UUID.fromString("4abe9f6d-6df3-6e5c-953e-3695db9a5216"))).thenReturn(invitations)

        mockMvc
            .perform(get("/api/v1/auth/invitations"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].email").value("user1@example.com"))
            .andExpect(jsonPath("$[0].role").value("MEMBER"))
            .andExpect(jsonPath("$[0].status").value("PENDING"))
    }

    @Test
    fun `POST invitations validate should return invitation details`() {
        val response =
            ValidateInvitationResponse(
                email = "invited@example.com",
                role = "MEMBER",
                organizationId = java.util.UUID.fromString("4abe9f6d-6df3-6e5c-953e-3695db9a5216"),
                existingUser = false,
            )
        `when`(invitationService.validateInvitation(any())).thenReturn(response)

        mockMvc
            .perform(
                post("/api/v1/auth/invitations/validate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"token": "valid-token"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value("invited@example.com"))
            .andExpect(jsonPath("$.role").value("MEMBER"))
            .andExpect(jsonPath("$.existingUser").value(false))
    }

    @Test
    fun `POST invitations validate should return 400 for invalid token`() {
        `when`(invitationService.validateInvitation(any()))
            .thenThrow(IllegalArgumentException("Invalid or expired invitation token"))

        mockMvc
            .perform(
                post("/api/v1/auth/invitations/validate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"token": "bad-token"}"""),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Invalid or expired invitation token"))
    }

    @Test
    fun `POST invitations accept should return 200 when accepted`() {
        `when`(invitationService.acceptInvitation(any())).thenReturn(testUser)

        mockMvc
            .perform(
                post("/api/v1/auth/invitations/accept")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                            "token": "valid-token",
                            "username": "newuser",
                            "password": "SecurePass123!",
                            "firstName": "New",
                            "lastName": "User"
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Invitation accepted successfully"))
    }

    @Test
    fun `POST invitations accept should return 400 with invalid token`() {
        `when`(invitationService.acceptInvitation(any()))
            .thenThrow(IllegalArgumentException("Invalid or expired invitation token"))

        mockMvc
            .perform(
                post("/api/v1/auth/invitations/accept")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                            "token": "invalid",
                            "username": "newuser",
                            "password": "SecurePass123!",
                            "firstName": "New",
                            "lastName": "User"
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Invalid or expired invitation token"))
    }

    @Test
    fun `DELETE invitation should return 200 when revoked`() {
        mockMvc
            .perform(delete("/api/v1/auth/invitations/52a14436-99e0-5e9d-9396-3a670fc505c0"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Invitation revoked"))
    }

    @Test
    fun `DELETE invitation should return 400 when not pending`() {
        `when`(invitationService.revokeInvitation(any(), any()))
            .thenThrow(IllegalArgumentException("Invitation is not pending"))

        mockMvc
            .perform(delete("/api/v1/auth/invitations/52a14436-99e0-5e9d-9396-3a670fc505c0"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Invitation is not pending"))
    }

    @Test
    fun `POST invitations should return 403 without invitation write permission`() {
        setupAuthWithPermissions("invitation:read")

        mockMvc
            .perform(
                post("/api/v1/auth/invitations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"email": "user@example.com", "role": "MEMBER"}"""),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `GET invitations should return 403 without invitation read permission`() {
        setupAuthWithPermissions()

        mockMvc
            .perform(get("/api/v1/auth/invitations"))
            .andExpect(status().isForbidden)
    }
}
