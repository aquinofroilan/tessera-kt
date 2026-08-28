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
import com.aquinofroilan.tessera.domain.sales.dto.PriceListLineDto
import com.aquinofroilan.tessera.domain.sales.dto.PriceListResponse
import com.aquinofroilan.tessera.domain.sales.model.CustomerSegment
import com.aquinofroilan.tessera.domain.sales.service.PriceListService
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@WebMvcTest(controllers = [PriceListController::class])
@Import(WebMvcConfig::class, LoggingAspect::class, TestSecurityConfig::class, TesseraPermissionEvaluator::class)
@ActiveProfiles("test")
class PriceListControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var priceListService: PriceListService

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
    private val testPriceListId = UUID.fromString("22222222-3333-4444-5555-666666666666")
    private val testProductId = UUID.fromString("11111111-2222-3333-4444-555555555555")

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

    private fun createPriceListResponse() =
        PriceListResponse(
            id = testPriceListId,
            organizationId = testOrgId,
            name = "Wholesale USD",
            code = "PL-WHOLESALE-USD",
            currency = "USD",
            customerSegment = CustomerSegment.WHOLESALE,
            isDefault = true,
            isActive = true,
            validFrom = null,
            validTo = null,
            description = "Standard wholesale",
            lines =
                listOf(
                    PriceListLineDto(
                        id = UUID.randomUUID(),
                        productId = testProductId,
                        productSku = "SKU-001",
                        unitPrice = BigDecimal("85.00"),
                        minQuantity = BigDecimal("10.0"),
                    ),
                ),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

    @Test
    fun `GET price lists should return 200 with list`() {
        `when`(priceListService.listPriceLists(eq(testOrgId), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(listOf(createPriceListResponse()))

        mockMvc
            .perform(get("/api/v1/sales/price-lists"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(testPriceListId.toString()))
            .andExpect(jsonPath("$[0].code").value("PL-WHOLESALE-USD"))
    }

    @Test
    fun `GET single price list should return 200`() {
        `when`(priceListService.getPriceList(testPriceListId, testOrgId)).thenReturn(createPriceListResponse())

        mockMvc
            .perform(get("/api/v1/sales/price-lists/$testPriceListId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(testPriceListId.toString()))
            .andExpect(jsonPath("$.lines[0].unitPrice").value(85.0))
    }

    @Test
    fun `POST price list should return 201`() {
        `when`(priceListService.createPriceList(eq(testOrgId), any())).thenReturn(createPriceListResponse())

        mockMvc
            .perform(
                post("/api/v1/sales/price-lists")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                            "name": "Wholesale USD",
                            "code": "PL-WHOLESALE-USD",
                            "currency": "USD",
                            "customerSegment": "WHOLESALE",
                            "isDefault": true
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(testPriceListId.toString()))
    }

    @Test
    fun `PUT price list should return 200`() {
        `when`(priceListService.updatePriceList(eq(testPriceListId), eq(testOrgId), any()))
            .thenReturn(createPriceListResponse())

        mockMvc
            .perform(
                put("/api/v1/sales/price-lists/$testPriceListId")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                            "name": "Updated Name"
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(testPriceListId.toString()))
    }

    @Test
    fun `DELETE price list should return 204`() {
        mockMvc
            .perform(delete("/api/v1/sales/price-lists/$testPriceListId"))
            .andExpect(status().isNoContent)
    }
}
