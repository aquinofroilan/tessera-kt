package com.aquinofroilan.tessera.domain.organization.controller

import com.aquinofroilan.tessera.aspect.LoggingAspect
import com.aquinofroilan.tessera.config.TestSecurityConfig
import com.aquinofroilan.tessera.config.WebMvcConfig
import com.aquinofroilan.tessera.domain.auth.model.RoleAssignment
import com.aquinofroilan.tessera.domain.auth.model.User
import com.aquinofroilan.tessera.domain.auth.repository.InvitationRepository
import com.aquinofroilan.tessera.domain.auth.repository.PasswordResetTokenRepository
import com.aquinofroilan.tessera.domain.auth.repository.RefreshTokenRepository
import com.aquinofroilan.tessera.domain.auth.repository.SessionTokenRepository
import com.aquinofroilan.tessera.domain.auth.repository.UserRepository
import com.aquinofroilan.tessera.domain.auth.service.ApiKeyService
import com.aquinofroilan.tessera.domain.auth.service.AuthService
import com.aquinofroilan.tessera.domain.finance.service.AccountService
import com.aquinofroilan.tessera.domain.finance.service.JournalEntryService
import com.aquinofroilan.tessera.domain.organization.dto.OrganizationStatusResponse
import com.aquinofroilan.tessera.domain.organization.model.OrganizationStatus
import com.aquinofroilan.tessera.domain.organization.repository.OrganizationRepository
import com.aquinofroilan.tessera.domain.organization.service.OrganizationLifecycleService
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.security.RolePermissionCache
import com.aquinofroilan.tessera.security.SessionContext
import com.aquinofroilan.tessera.security.TesseraPermissionEvaluator
import com.aquinofroilan.tessera.util.TokenHasher
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@WebMvcTest(controllers = [OrganizationLifecycleController::class])
@Import(WebMvcConfig::class, LoggingAspect::class, TestSecurityConfig::class, TesseraPermissionEvaluator::class)
@ActiveProfiles("test")
class OrganizationLifecycleControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var lifecycleService: OrganizationLifecycleService

    @MockitoBean
    private lateinit var authService: AuthService

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
    private lateinit var apiKeyService: ApiKeyService

    @MockitoBean
    private lateinit var accountService: AccountService

    @MockitoBean
    private lateinit var journalEntryService: JournalEntryService

    @MockitoBean
    private lateinit var authenticationContext: AuthenticationContext

    private val testOrgId = UUID.fromString("4abe9f6d-6df3-6e5c-953e-3695db9a5216")
    private val testUserId = UUID.fromString("bc17c97c-3d89-7d43-b7e0-7ca0266eafa8")

    private val testUser =
        User(
            uuid = testUserId,
            username = "testowner",
            email = "owner@example.com",
            firstName = "Test",
            lastName = "Owner",
            passwordHash = "encoded",
            organizationId = testOrgId,
            roleAssignments = listOf(RoleAssignment("OWNER", testOrgId)),
        )

    @BeforeEach
    fun setup() {
        setupAuthWithPermissions("organization:read", "organization:write")
        `when`(authenticationContext.organizationId()).thenReturn(testOrgId)
        `when`(authenticationContext.userId()).thenReturn(testUserId)
    }

    private fun setupAuthWithPermissions(vararg permissions: String) {
        val roleAuthorities = testUser.roleAssignments.map { SimpleGrantedAuthority("ROLE_${it.role}") }
        val permissionAuthorities = permissions.map { SimpleGrantedAuthority(it) }
        val authentication = UsernamePasswordAuthenticationToken(testUser, null, roleAuthorities + permissionAuthorities)
        authentication.details =
            SessionContext(
                sessionId = UUID.fromString("79c5ca4c-8e48-a8f8-6ffc-5b3271a250aa"),
                organizationId = testOrgId,
            )
        SecurityContextHolder.getContext().authentication = authentication
    }

    private fun createStatusResponse(status: OrganizationStatus = OrganizationStatus.ACTIVE) =
        OrganizationStatusResponse(
            organizationId = testOrgId,
            orgSlug = "acme",
            name = "Acme Corp",
            status = status,
            readOnly = status == OrganizationStatus.ARCHIVED,
            accessBlocked = status == OrganizationStatus.SUSPENDED,
            allowedTransitions =
                when (status) {
                    OrganizationStatus.ACTIVE -> listOf(OrganizationStatus.SUSPENDED, OrganizationStatus.ARCHIVED)
                    OrganizationStatus.SUSPENDED -> listOf(OrganizationStatus.ACTIVE, OrganizationStatus.ARCHIVED)
                    OrganizationStatus.ARCHIVED -> listOf(OrganizationStatus.ACTIVE, OrganizationStatus.SUSPENDED)
                },
        )

    @Test
    fun `GET status should return 200 with organization status`() {
        `when`(lifecycleService.getStatus(testOrgId)).thenReturn(createStatusResponse())

        mockMvc
            .perform(get("/api/v1/organization/status"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.organizationId").value(testOrgId.toString()))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.readOnly").value(false))
            .andExpect(jsonPath("$.accessBlocked").value(false))
            .andExpect(jsonPath("$.allowedTransitions.length()").value(2))
    }

    @Test
    fun `POST suspend should return 200 with SUSPENDED status`() {
        `when`(lifecycleService.transitionStatus(eq(testOrgId), eq(OrganizationStatus.SUSPENDED), anyOrNull()))
            .thenReturn(createStatusResponse(OrganizationStatus.SUSPENDED))

        mockMvc
            .perform(
                post("/api/v1/organization/status/suspend")
                    .param("reason", "Non-payment"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("SUSPENDED"))
            .andExpect(jsonPath("$.accessBlocked").value(true))
    }

    @Test
    fun `POST archive should return 200 with ARCHIVED status`() {
        `when`(lifecycleService.transitionStatus(eq(testOrgId), eq(OrganizationStatus.ARCHIVED), anyOrNull()))
            .thenReturn(createStatusResponse(OrganizationStatus.ARCHIVED))

        mockMvc
            .perform(post("/api/v1/organization/status/archive"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("ARCHIVED"))
            .andExpect(jsonPath("$.readOnly").value(true))
    }

    @Test
    fun `POST activate should return 200 with ACTIVE status`() {
        `when`(lifecycleService.transitionStatus(eq(testOrgId), eq(OrganizationStatus.ACTIVE), anyOrNull()))
            .thenReturn(createStatusResponse(OrganizationStatus.ACTIVE))

        mockMvc
            .perform(post("/api/v1/organization/status/activate"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("ACTIVE"))
    }

    @Test
    fun `POST transition with body should return 200`() {
        `when`(lifecycleService.transitionStatus(eq(testOrgId), eq(OrganizationStatus.SUSPENDED), eq("Audit")))
            .thenReturn(createStatusResponse(OrganizationStatus.SUSPENDED))

        mockMvc
            .perform(
                post("/api/v1/organization/status")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"targetStatus": "SUSPENDED", "reason": "Audit"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("SUSPENDED"))
    }

    @Test
    fun `GET status should return 403 when missing organization read`() {
        setupAuthWithPermissions("inventory:read")

        mockMvc
            .perform(get("/api/v1/organization/status"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `POST suspend should return 403 when missing organization write`() {
        setupAuthWithPermissions("organization:read")

        mockMvc
            .perform(post("/api/v1/organization/status/suspend"))
            .andExpect(status().isForbidden)
    }
}
