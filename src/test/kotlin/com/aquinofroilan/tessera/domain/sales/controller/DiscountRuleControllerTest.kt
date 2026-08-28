package com.aquinofroilan.tessera.domain.sales.controller

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
import com.aquinofroilan.tessera.domain.sales.dto.DiscountRuleResponse
import com.aquinofroilan.tessera.domain.sales.model.CustomerSegment
import com.aquinofroilan.tessera.domain.sales.model.DiscountType
import com.aquinofroilan.tessera.domain.sales.service.DiscountRuleService
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
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@WebMvcTest(controllers = [DiscountRuleController::class])
@Import(WebMvcConfig::class, LoggingAspect::class, TestSecurityConfig::class, TesseraPermissionEvaluator::class)
@ActiveProfiles("test")
class DiscountRuleControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var discountRuleService: DiscountRuleService

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
    private val testRuleId = UUID.fromString("33333333-4444-5555-6666-777777777777")

    private val testUser =
        User(
            uuid = testUserId,
            username = "testsales",
            email = "sales@example.com",
            firstName = "Test",
            lastName = "Sales",
            passwordHash = "encoded",
            organizationId = testOrgId,
            roleAssignments = listOf(RoleAssignment("SALES_MANAGER", testOrgId)),
        )

    @BeforeEach
    fun setup() {
        val roleAuthorities = testUser.roleAssignments.map { SimpleGrantedAuthority("ROLE_${it.role}") }
        val permissionAuthorities = listOf("sales:read", "sales:create").map { SimpleGrantedAuthority(it) }
        val authentication = UsernamePasswordAuthenticationToken(testUser, null, roleAuthorities + permissionAuthorities)
        authentication.details =
            SessionContext(
                sessionId = UUID.fromString("79c5ca4c-8e48-a8f8-6ffc-5b3271a250aa"),
                organizationId = testOrgId,
            )
        SecurityContextHolder.getContext().authentication = authentication
        `when`(authenticationContext.organizationId()).thenReturn(testOrgId)
        `when`(authenticationContext.userId()).thenReturn(testUserId)
    }

    private fun createRuleResponse() =
        DiscountRuleResponse(
            id = testRuleId,
            organizationId = testOrgId,
            name = "VIP 10% Off",
            code = "DISC-VIP-10",
            discountType = DiscountType.PERCENTAGE,
            discountValue = BigDecimal("10.00"),
            customerSegment = CustomerSegment.VIP,
            customerId = null,
            productId = null,
            priceListId = null,
            minQuantity = null,
            minOrderAmount = null,
            validFrom = null,
            validTo = null,
            isActive = true,
            priority = 10,
            description = "VIP discount",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

    @Test
    fun `GET discount rules should return 200 with list`() {
        `when`(discountRuleService.listDiscountRules(testOrgId)).thenReturn(listOf(createRuleResponse()))

        mockMvc
            .perform(get("/api/v1/sales/discount-rules"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(testRuleId.toString()))
            .andExpect(jsonPath("$[0].code").value("DISC-VIP-10"))
    }

    @Test
    fun `GET single discount rule should return 200`() {
        `when`(discountRuleService.getDiscountRule(testRuleId, testOrgId)).thenReturn(createRuleResponse())

        mockMvc
            .perform(get("/api/v1/sales/discount-rules/$testRuleId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(testRuleId.toString()))
            .andExpect(jsonPath("$.discountType").value("PERCENTAGE"))
    }

    @Test
    fun `POST discount rule should return 201`() {
        `when`(discountRuleService.createDiscountRule(eq(testOrgId), any())).thenReturn(createRuleResponse())

        mockMvc
            .perform(
                post("/api/v1/sales/discount-rules")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                            "name": "VIP 10% Off",
                            "code": "DISC-VIP-10",
                            "discountType": "PERCENTAGE",
                            "discountValue": 10.0,
                            "customerSegment": "VIP"
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(testRuleId.toString()))
    }

    @Test
    fun `PUT discount rule should return 200`() {
        `when`(discountRuleService.updateDiscountRule(eq(testRuleId), eq(testOrgId), any()))
            .thenReturn(createRuleResponse())

        mockMvc
            .perform(
                put("/api/v1/sales/discount-rules/$testRuleId")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                            "name": "Updated VIP"
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(testRuleId.toString()))
    }

    @Test
    fun `DELETE discount rule should return 204`() {
        mockMvc
            .perform(delete("/api/v1/sales/discount-rules/$testRuleId"))
            .andExpect(status().isNoContent)
    }
}
