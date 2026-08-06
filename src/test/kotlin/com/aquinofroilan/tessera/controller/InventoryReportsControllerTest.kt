package com.aquinofroilan.tessera.controller

import com.aquinofroilan.tessera.aspect.LoggingAspect
import com.aquinofroilan.tessera.config.TestSecurityConfig
import com.aquinofroilan.tessera.dto.MovementHistoryLineResponse
import com.aquinofroilan.tessera.dto.MovementHistoryResponse
import com.aquinofroilan.tessera.dto.StockOnHandLineResponse
import com.aquinofroilan.tessera.dto.StockOnHandReportResponse
import com.aquinofroilan.tessera.dto.ValuationLineResponse
import com.aquinofroilan.tessera.dto.ValuationReportResponse
import com.aquinofroilan.tessera.model.InventoryCostingMethod
import com.aquinofroilan.tessera.model.RoleAssignment
import com.aquinofroilan.tessera.model.StockMovementType
import com.aquinofroilan.tessera.model.User
import com.aquinofroilan.tessera.repository.InvitationRepository
import com.aquinofroilan.tessera.repository.OrganizationRepository
import com.aquinofroilan.tessera.repository.PasswordResetTokenRepository
import com.aquinofroilan.tessera.repository.RefreshTokenRepository
import com.aquinofroilan.tessera.repository.SessionTokenRepository
import com.aquinofroilan.tessera.repository.UserRepository
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.security.RolePermissionCache
import com.aquinofroilan.tessera.security.SessionContext
import com.aquinofroilan.tessera.security.TesseraPermissionEvaluator
import com.aquinofroilan.tessera.service.AccountService
import com.aquinofroilan.tessera.service.ApiKeyService
import com.aquinofroilan.tessera.service.AuthService
import com.aquinofroilan.tessera.service.InventoryReorderRuleService
import com.aquinofroilan.tessera.service.InventoryReportsService
import com.aquinofroilan.tessera.service.InventoryValuationService
import com.aquinofroilan.tessera.service.JournalEntryService
import com.aquinofroilan.tessera.util.TokenHasher
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@WebMvcTest(controllers = [InventoryReportsController::class])
@Import(LoggingAspect::class, TestSecurityConfig::class, TesseraPermissionEvaluator::class)
@ActiveProfiles("test")
class InventoryReportsControllerTest {
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
    private lateinit var inventoryValuationService: InventoryValuationService

    @MockitoBean
    private lateinit var inventoryReportsService: InventoryReportsService

    @MockitoBean
    private lateinit var reorderRuleService: InventoryReorderRuleService

    @MockitoBean
    private lateinit var authenticationContext: AuthenticationContext

    private val testUser =
        User(
            uuid = UUID.fromString("00000000-0000-0000-0000-000000000123"),
            username = "testuser",
            email = "test@example.com",
            firstName = "Test",
            lastName = "User",
            passwordHash = "encoded",
            organizationId = UUID.fromString("00000000-0000-0000-0000-000000000124"),
            roleAssignments = listOf(RoleAssignment("OWNER", UUID.fromString("00000000-0000-0000-0000-000000000124"))),
        )

    @BeforeEach
    fun setup() {
        setupAuthWithPermissions("inventory:read")
        `when`(authenticationContext.organizationId()).thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000124"))
    }

    private fun setupAuthWithPermissions(vararg permissions: String) {
        val roleAuthorities = testUser.roleAssignments.map { SimpleGrantedAuthority("ROLE_${it.role}") }
        val permissionAuthorities = permissions.map { SimpleGrantedAuthority(it) }
        val authentication = UsernamePasswordAuthenticationToken(testUser, null, roleAuthorities + permissionAuthorities)
        authentication.details =
            SessionContext(
                sessionId = UUID.fromString("00000000-0000-0000-0000-000000000125"),
                organizationId = UUID.fromString("00000000-0000-0000-0000-000000000124"),
            )
        SecurityContextHolder.getContext().authentication = authentication
    }

    @Test
    fun `GET valuation should return 200 with report`() {
        `when`(inventoryValuationService.valuation(any())).thenReturn(
            ValuationReportResponse(
                costingMethod = InventoryCostingMethod.WEIGHTED_AVERAGE,
                lines =
                    listOf(
                        ValuationLineResponse(
                            UUID.fromString("00000000-0000-0000-0000-000000000999"),
                            UUID.fromString("00000000-0000-0000-0000-000000000999"),
                            BigDecimal("10"),
                            BigDecimal("50"),
                        ),
                    ),
                totalValue = BigDecimal("50"),
            ),
        )
        mockMvc
            .perform(get("/inventory/reports/valuation"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.costingMethod").value("WEIGHTED_AVERAGE"))
            .andExpect(jsonPath("$.totalValue").value(50))
    }

    @Test
    fun `GET valuation requires inventory read`() {
        setupAuthWithPermissions("inventory:write")
        mockMvc
            .perform(get("/inventory/reports/valuation"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `GET stock-on-hand returns 200 with lines`() {
        `when`(inventoryReportsService.stockOnHand(any(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(
            StockOnHandReportResponse(
                asOfDate = null,
                lines =
                    listOf(
                        StockOnHandLineResponse(
                            UUID.fromString("00000000-0000-0000-0000-000000000999"),
                            UUID.fromString("00000000-0000-0000-0000-000000000999"),
                            BigDecimal("10"),
                        ),
                        StockOnHandLineResponse(
                            UUID.fromString("00000000-0000-0000-0000-000000000998"),
                            UUID.fromString("00000000-0000-0000-0000-000000000999"),
                            BigDecimal("5"),
                        ),
                    ),
            ),
        )
        mockMvc
            .perform(get("/inventory/reports/stock-on-hand"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.lines.length()").value(2))
    }

    @Test
    fun `GET stock-on-hand passes filters through`() {
        `when`(inventoryReportsService.stockOnHand(any(), any(), any(), any())).thenReturn(
            StockOnHandReportResponse(
                asOfDate = "2026-05-01T00:00:00",
                lines =
                    listOf(
                        StockOnHandLineResponse(
                            UUID.fromString("00000000-0000-0000-0000-000000000999"),
                            UUID.fromString("00000000-0000-0000-0000-000000000999"),
                            BigDecimal("10"),
                        ),
                    ),
            ),
        )
        mockMvc
            .perform(
                get("/inventory/reports/stock-on-hand")
                    .param("productId", "p-1")
                    .param("warehouseId", "wh-1")
                    .param("asOfDate", "2026-05-01T00:00:00"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.lines[0].productId").value("00000000-0000-0000-0000-000000000999"))
    }

    @Test
    fun `GET movements returns 200 with running balance`() {
        `when`(
            inventoryReportsService.movementHistory(any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()),
        ).thenReturn(
            MovementHistoryResponse(
                productId = null,
                warehouseId = null,
                from = null,
                to = null,
                lines =
                    listOf(
                        MovementHistoryLineResponse(
                            id = UUID.fromString("00000000-0000-0000-0000-000000000999"),
                            type = StockMovementType.RECEIPT,
                            productId = UUID.fromString("00000000-0000-0000-0000-000000000999"),
                            warehouseId = UUID.fromString("00000000-0000-0000-0000-000000000999"),
                            transferToWarehouseId = null,
                            quantity = BigDecimal("10"),
                            unitCost = BigDecimal("5"),
                            occurredAt = LocalDateTime.now().toString(),
                            runningBalance = BigDecimal("10"),
                        ),
                    ),
            ),
        )
        mockMvc
            .perform(get("/inventory/reports/movements"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.lines.length()").value(1))
            .andExpect(jsonPath("$.lines[0].runningBalance").value(10))
    }

    @Test
    fun `GET stock-on-hand requires inventory read`() {
        setupAuthWithPermissions("inventory:write")
        mockMvc
            .perform(get("/inventory/reports/stock-on-hand"))
            .andExpect(status().isForbidden)
    }
}
