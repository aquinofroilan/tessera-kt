package com.aquinofroilan.tessera.domain.procurement.controller

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
import com.aquinofroilan.tessera.domain.organization.repository.OrganizationRepository
import com.aquinofroilan.tessera.domain.procurement.dto.VendorEvaluationResponse
import com.aquinofroilan.tessera.domain.procurement.dto.VendorPerformanceSummaryResponse
import com.aquinofroilan.tessera.domain.procurement.service.VendorPerformanceService
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.security.RolePermissionCache
import com.aquinofroilan.tessera.security.SessionContext
import com.aquinofroilan.tessera.security.TesseraPermissionEvaluator
import com.aquinofroilan.tessera.util.TokenHasher
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@WebMvcTest(controllers = [VendorPerformanceController::class])
@Import(WebMvcConfig::class, LoggingAspect::class, TestSecurityConfig::class, TesseraPermissionEvaluator::class)
@ActiveProfiles("test")
class VendorPerformanceControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var vendorPerformanceService: VendorPerformanceService

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
    private val testVendorId = UUID.fromString("11111111-2222-3333-4444-555555555555")
    private val testEvalId = UUID.fromString("22222222-3333-4444-5555-666666666666")

    private val testUser =
        User(
            uuid = testUserId,
            username = "testbuyer",
            email = "buyer@example.com",
            firstName = "Test",
            lastName = "Buyer",
            passwordHash = "encoded",
            organizationId = testOrgId,
            roleAssignments = listOf(RoleAssignment("BUYER", testOrgId)),
        )

    @BeforeEach
    fun setup() {
        setupAuthWithPermissions("ap:read", "ap:create")
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

    private fun createSummary() =
        VendorPerformanceSummaryResponse(
            vendorId = testVendorId,
            vendorName = "Acme Supplies",
            totalOrders = 5,
            completedOrders = 4,
            totalSpend = BigDecimal("5000.00"),
            onTimeDeliveryRate = 80.0,
            averageDeliveryDelayDays = 2.0,
            qualityFulfillmentRate = 95.0,
            priceAccuracyRate = 100.0,
            deliveryScore = 76.0,
            qualityScore = 95.0,
            priceAccuracyScore = 100.0,
            overallScore = 88.65,
            ratingTier = "GOOD",
            totalEvaluations = 1,
            evaluationAverageScore = 90.0,
        )

    private fun createEvaluation() =
        VendorEvaluationResponse(
            id = testEvalId,
            vendorId = testVendorId,
            organizationId = testOrgId,
            purchaseOrderId = null,
            evaluationDate = LocalDate.of(2026, 2, 1),
            deliveryScore = BigDecimal("90.00"),
            qualityScore = BigDecimal("85.00"),
            priceAccuracyScore = BigDecimal("95.00"),
            overallScore = BigDecimal("89.50"),
            comments = "Good vendor",
            evaluatedBy = testUserId,
            createdAt = LocalDateTime.of(2026, 2, 1, 10, 0),
        )

    @Test
    fun `GET performance should return 200 with list of vendor performances`() {
        `when`(vendorPerformanceService.listAllVendorPerformance(eq(testOrgId), anyOrNull(), anyOrNull()))
            .thenReturn(listOf(createSummary()))

        mockMvc
            .perform(get("/api/v1/procurement/vendors/performance"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].vendorId").value(testVendorId.toString()))
            .andExpect(jsonPath("$[0].vendorName").value("Acme Supplies"))
            .andExpect(jsonPath("$[0].overallScore").value(88.65))
            .andExpect(jsonPath("$[0].ratingTier").value("GOOD"))
    }

    @Test
    fun `GET single vendor performance should return 200 with summary`() {
        `when`(vendorPerformanceService.getVendorPerformance(eq(testVendorId), eq(testOrgId), anyOrNull(), anyOrNull()))
            .thenReturn(createSummary())

        mockMvc
            .perform(get("/api/v1/procurement/vendors/$testVendorId/performance"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.vendorId").value(testVendorId.toString()))
            .andExpect(jsonPath("$.onTimeDeliveryRate").value(80.0))
            .andExpect(jsonPath("$.qualityFulfillmentRate").value(95.0))
    }

    @Test
    fun `POST evaluation should return 201 with created evaluation`() {
        `when`(vendorPerformanceService.recordEvaluation(eq(testVendorId), eq(testOrgId), eq(testUserId), any()))
            .thenReturn(createEvaluation())

        mockMvc
            .perform(
                post("/api/v1/procurement/vendors/$testVendorId/evaluations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                            "deliveryScore": 90.0,
                            "qualityScore": 85.0,
                            "priceAccuracyScore": 95.0,
                            "comments": "Good vendor"
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(testEvalId.toString()))
            .andExpect(jsonPath("$.overallScore").value(89.5))
    }

    @Test
    fun `GET evaluations should return 200 with list of evaluations`() {
        `when`(vendorPerformanceService.listEvaluations(testVendorId, testOrgId)).thenReturn(listOf(createEvaluation()))

        mockMvc
            .perform(get("/api/v1/procurement/vendors/$testVendorId/evaluations"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(testEvalId.toString()))
    }

    @Test
    fun `DELETE evaluation should return 204`() {
        mockMvc
            .perform(delete("/api/v1/procurement/vendors/$testVendorId/evaluations/$testEvalId"))
            .andExpect(status().isNoContent)
    }
}
