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
import com.aquinofroilan.tessera.domain.organization.dto.EffectiveFeaturesResponse
import com.aquinofroilan.tessera.domain.organization.dto.FeatureFlagDetailDto
import com.aquinofroilan.tessera.domain.organization.dto.OrganizationPlanResponse
import com.aquinofroilan.tessera.domain.organization.model.BillingPlan
import com.aquinofroilan.tessera.domain.organization.repository.OrganizationRepository
import com.aquinofroilan.tessera.domain.organization.service.OrganizationBillingFeatureService
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@WebMvcTest(controllers = [OrganizationBillingFeatureController::class])
@Import(WebMvcConfig::class, LoggingAspect::class, TestSecurityConfig::class, TesseraPermissionEvaluator::class)
@ActiveProfiles("test")
class OrganizationBillingFeatureControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var billingFeatureService: OrganizationBillingFeatureService

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

    private fun createPlanResponse(plan: BillingPlan = BillingPlan.STARTER) =
        OrganizationPlanResponse(
            organizationId = testOrgId,
            orgSlug = "acme",
            name = "Acme Corp",
            billingPlan = plan,
            effectiveFeatures = mapOf("API_KEYS" to true, "ADVANCED_ANALYTICS" to false),
            featureOverrides = emptyMap(),
        )

    @Test
    fun `GET plan should return 200 with organization billing plan`() {
        `when`(billingFeatureService.getPlan(testOrgId)).thenReturn(createPlanResponse())

        mockMvc
            .perform(get("/api/v1/organization/billing/plan"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.organizationId").value(testOrgId.toString()))
            .andExpect(jsonPath("$.billingPlan").value("STARTER"))
            .andExpect(jsonPath("$.effectiveFeatures.API_KEYS").value(true))
    }

    @Test
    fun `PUT plan should return 200 when plan is updated`() {
        val updatedResponse = createPlanResponse(BillingPlan.ENTERPRISE)
        `when`(billingFeatureService.updatePlan(eq(testOrgId), eq(BillingPlan.ENTERPRISE))).thenReturn(updatedResponse)

        mockMvc
            .perform(
                put("/api/v1/organization/billing/plan")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"plan": "ENTERPRISE"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.billingPlan").value("ENTERPRISE"))
    }

    @Test
    fun `GET features should return 200 with effective features list`() {
        val effectiveResponse =
            EffectiveFeaturesResponse(
                organizationId = testOrgId,
                billingPlan = BillingPlan.STARTER,
                features =
                    listOf(
                        FeatureFlagDetailDto(
                            featureKey = "API_KEYS",
                            enabled = true,
                            isOverride = false,
                            planDefault = true,
                        ),
                    ),
            )
        `when`(billingFeatureService.getEffectiveFeatures(testOrgId)).thenReturn(effectiveResponse)

        mockMvc
            .perform(get("/api/v1/organization/billing/features"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.billingPlan").value("STARTER"))
            .andExpect(jsonPath("$.features[0].featureKey").value("API_KEYS"))
            .andExpect(jsonPath("$.features[0].enabled").value(true))
    }

    @Test
    fun `PUT feature override should return 200`() {
        val detail =
            FeatureFlagDetailDto(
                featureKey = "ADVANCED_ANALYTICS",
                enabled = true,
                isOverride = true,
                planDefault = false,
            )
        `when`(billingFeatureService.setFeatureOverride(eq(testOrgId), eq("ADVANCED_ANALYTICS"), eq(true)))
            .thenReturn(detail)

        mockMvc
            .perform(
                put("/api/v1/organization/billing/features/ADVANCED_ANALYTICS")
                    .param("enabled", "true"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.featureKey").value("ADVANCED_ANALYTICS"))
            .andExpect(jsonPath("$.enabled").value(true))
            .andExpect(jsonPath("$.override").value(true))
    }

    @Test
    fun `DELETE feature override should return 200`() {
        val detail =
            FeatureFlagDetailDto(
                featureKey = "API_KEYS",
                enabled = true,
                isOverride = false,
                planDefault = true,
            )
        `when`(billingFeatureService.removeFeatureOverride(eq(testOrgId), eq("API_KEYS"))).thenReturn(detail)

        mockMvc
            .perform(delete("/api/v1/organization/billing/features/API_KEYS"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.featureKey").value("API_KEYS"))
            .andExpect(jsonPath("$.override").value(false))
    }

    @Test
    fun `POST batch overrides should return 200`() {
        val effectiveResponse =
            EffectiveFeaturesResponse(
                organizationId = testOrgId,
                billingPlan = BillingPlan.FREE,
                features = emptyList(),
            )
        `when`(billingFeatureService.batchSetOverrides(eq(testOrgId), any())).thenReturn(effectiveResponse)

        mockMvc
            .perform(
                post("/api/v1/organization/billing/features/batch")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"overrides": {"API_KEYS": true}}"""),
            ).andExpect(status().isOk)
    }

    @Test
    fun `GET plan should return 403 when missing organization read`() {
        setupAuthWithPermissions("inventory:read")

        mockMvc
            .perform(get("/api/v1/organization/billing/plan"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `PUT plan should return 403 when missing organization write`() {
        setupAuthWithPermissions("organization:read")

        mockMvc
            .perform(
                put("/api/v1/organization/billing/plan")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"plan": "ENTERPRISE"}"""),
            ).andExpect(status().isForbidden)
    }
}
