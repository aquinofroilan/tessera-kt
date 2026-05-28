package com.loom.synectix.controller

import com.loom.synectix.aspect.LoggingAspect
import com.loom.synectix.config.TestSecurityConfig
import com.loom.synectix.dto.ValidateInvitationResponse
import com.loom.synectix.model.Invitation
import com.loom.synectix.model.RoleAssignment
import com.loom.synectix.model.User
import com.loom.synectix.repository.InvitationRepository
import com.loom.synectix.repository.OrganizationRepository
import com.loom.synectix.repository.PasswordResetTokenRepository
import com.loom.synectix.repository.RefreshTokenRepository
import com.loom.synectix.repository.SessionTokenRepository
import com.loom.synectix.repository.UserRepository
import com.loom.synectix.security.AuthenticationContext
import com.loom.synectix.security.RolePermissionCache
import com.loom.synectix.security.SessionContext
import com.loom.synectix.security.SynectixPermissionEvaluator
import com.loom.synectix.service.ApiKeyService
import com.loom.synectix.service.InvitationService
import com.loom.synectix.util.TokenHasher
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

@WebMvcTest(controllers = [InvitationController::class])
@Import(LoggingAspect::class, TestSecurityConfig::class, SynectixPermissionEvaluator::class)
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
            uuid = "user-123",
            username = "testuser",
            email = "test@example.com",
            firstName = "Test",
            lastName = "User",
            passwordHash = "encoded",
            organizationId = "org-123",
            roleAssignments = listOf(RoleAssignment("OWNER", "org-123")),
        )

    @BeforeEach
    fun setup() {
        setupAuthWithPermissions("invitation:read", "invitation:write")
    }

    private fun setupAuthWithPermissions(vararg permissions: String) {
        val roleAuthorities = testUser.roleAssignments.map { SimpleGrantedAuthority("ROLE_${it.role}") }
        val permissionAuthorities = permissions.map { SimpleGrantedAuthority(it) }
        val authentication = UsernamePasswordAuthenticationToken(testUser, null, roleAuthorities + permissionAuthorities)
        authentication.details = SessionContext(sessionId = "session-123", organizationId = "org-123")
        SecurityContextHolder.getContext().authentication = authentication
    }

    @Test
    fun `POST invitations should return 201 when invitation is created`() {
        `when`(invitationService.invite(any(), any(), any())).thenReturn("raw-token-123")

        mockMvc
            .perform(
                post("/auth/invitations")
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
                post("/auth/invitations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"email": "not-an-email", "role": "MEMBER"}"""),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `POST invitations should return 400 when role is blank`() {
        mockMvc
            .perform(
                post("/auth/invitations")
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
                post("/auth/invitations")
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
                    id = "inv-1",
                    email = "user1@example.com",
                    organizationId = "org-123",
                    role = "MEMBER",
                    tokenHash = "hash1",
                    invitedBy = "user-123",
                    expiryAt = LocalDateTime.now(ZoneOffset.UTC).plusHours(72),
                ),
            )
        `when`(invitationService.listInvitations("org-123")).thenReturn(invitations)

        mockMvc
            .perform(get("/auth/invitations"))
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
                organizationId = "org-123",
                existingUser = false,
            )
        `when`(invitationService.validateInvitation(any())).thenReturn(response)

        mockMvc
            .perform(
                post("/auth/invitations/validate")
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
                post("/auth/invitations/validate")
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
                post("/auth/invitations/accept")
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
                post("/auth/invitations/accept")
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
            .perform(delete("/auth/invitations/inv-123"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Invitation revoked"))
    }

    @Test
    fun `DELETE invitation should return 400 when not pending`() {
        `when`(invitationService.revokeInvitation(any(), any()))
            .thenThrow(IllegalArgumentException("Invitation is not pending"))

        mockMvc
            .perform(delete("/auth/invitations/inv-123"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Invitation is not pending"))
    }

    @Test
    fun `POST invitations should return 403 without invitation write permission`() {
        setupAuthWithPermissions("invitation:read")

        mockMvc
            .perform(
                post("/auth/invitations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"email": "user@example.com", "role": "MEMBER"}"""),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `GET invitations should return 403 without invitation read permission`() {
        setupAuthWithPermissions()

        mockMvc
            .perform(get("/auth/invitations"))
            .andExpect(status().isForbidden)
    }
}
