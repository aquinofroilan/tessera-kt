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
import com.aquinofroilan.tessera.domain.organization.dto.OrganizationSettingsResponse
import com.aquinofroilan.tessera.domain.organization.model.InventoryCostingMethod
import com.aquinofroilan.tessera.domain.organization.repository.OrganizationRepository
import com.aquinofroilan.tessera.domain.organization.service.OrganizationSettingsService
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.security.RolePermissionCache
import com.aquinofroilan.tessera.security.SessionContext
import com.aquinofroilan.tessera.security.TesseraPermissionEvaluator
import com.aquinofroilan.tessera.util.TokenHasher
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.util.UUID

@WebMvcTest(controllers = [OrganizationSettingsController::class])
@Import(WebMvcConfig::class, LoggingAspect::class, TestSecurityConfig::class, TesseraPermissionEvaluator::class)
@ActiveProfiles("test")
class OrganizationSettingsControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var settingsService: OrganizationSettingsService

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

    private fun createSettingsResponse() =
        OrganizationSettingsResponse(
            id = testOrgId,
            orgSlug = "acme",
            name = "Acme Corp",
            description = "Acme Corporation ERP",
            legalName = "Acme Inc.",
            tradeName = "Acme",
            baseCurrency = "USD",
            fiscalYearStart = LocalDateTime.of(2026, 1, 1, 0, 0),
            timezone = "UTC",
            logoUrl = "https://example.com/logo.png",
            status = "ACTIVE",
            inventoryCostingMethod = InventoryCostingMethod.WEIGHTED_AVERAGE,
            inventoryGlPostingEnabled = true,
            createdAt = LocalDateTime.of(2026, 1, 1, 0, 0),
        )

    @Test
    fun `GET settings should return 200 with organization settings`() {
        `when`(settingsService.getSettings(testOrgId)).thenReturn(createSettingsResponse())

        mockMvc
            .perform(get("/api/v1/organization/settings"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(testOrgId.toString()))
            .andExpect(jsonPath("$.orgSlug").value("acme"))
            .andExpect(jsonPath("$.name").value("Acme Corp"))
            .andExpect(jsonPath("$.baseCurrency").value("USD"))
            .andExpect(jsonPath("$.timezone").value("UTC"))
            .andExpect(jsonPath("$.logoUrl").value("https://example.com/logo.png"))
            .andExpect(jsonPath("$.inventoryCostingMethod").value("WEIGHTED_AVERAGE"))
            .andExpect(jsonPath("$.inventoryGlPostingEnabled").value(true))
    }

    @Test
    fun `PUT settings should return 200 when settings are updated`() {
        val updatedResponse = createSettingsResponse().copy(name = "Acme Global", baseCurrency = "EUR")
        `when`(settingsService.updateSettings(eq(testOrgId), any())).thenReturn(updatedResponse)

        mockMvc
            .perform(
                put("/api/v1/organization/settings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{
                            "name": "Acme Global",
                            "baseCurrency": "EUR"
                        }""",
                    ),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Acme Global"))
            .andExpect(jsonPath("$.baseCurrency").value("EUR"))
    }

    @Test
    fun `PATCH settings should return 200 when settings are updated`() {
        val updatedResponse = createSettingsResponse().copy(logoUrl = "https://example.com/new.svg")
        `when`(settingsService.updateSettings(eq(testOrgId), any())).thenReturn(updatedResponse)

        mockMvc
            .perform(
                patch("/api/v1/organization/settings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"logoUrl": "https://example.com/new.svg"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.logoUrl").value("https://example.com/new.svg"))
    }

    @Test
    fun `GET settings should return 403 when missing organization read permission`() {
        setupAuthWithPermissions("organization:write")

        mockMvc
            .perform(get("/api/v1/organization/settings"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `PUT settings should return 403 when missing organization write permission`() {
        setupAuthWithPermissions("organization:read")

        mockMvc
            .perform(
                put("/api/v1/organization/settings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name": "New Name"}"""),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `PUT settings should return 400 when baseCurrency length is invalid`() {
        mockMvc
            .perform(
                put("/api/v1/organization/settings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"baseCurrency": "US"}"""),
            ).andExpect(status().isBadRequest)
    }
}
