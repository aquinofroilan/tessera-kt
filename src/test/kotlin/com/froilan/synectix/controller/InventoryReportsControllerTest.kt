package com.froilan.synectix.controller

import com.froilan.synectix.aspect.LoggingAspect
import com.froilan.synectix.config.TestSecurityConfig
import com.froilan.synectix.dto.MovementHistoryLineResponse
import com.froilan.synectix.dto.MovementHistoryResponse
import com.froilan.synectix.dto.StockOnHandLineResponse
import com.froilan.synectix.dto.StockOnHandReportResponse
import com.froilan.synectix.dto.ValuationLineResponse
import com.froilan.synectix.dto.ValuationReportResponse
import com.froilan.synectix.model.InventoryCostingMethod
import com.froilan.synectix.model.RoleAssignment
import com.froilan.synectix.model.StockMovementType
import com.froilan.synectix.model.User
import com.froilan.synectix.repository.InvitationRepository
import com.froilan.synectix.repository.OrganizationRepository
import com.froilan.synectix.repository.PasswordResetTokenRepository
import com.froilan.synectix.repository.RefreshTokenRepository
import com.froilan.synectix.repository.SessionTokenRepository
import com.froilan.synectix.repository.UserRepository
import com.froilan.synectix.security.AuthenticationContext
import com.froilan.synectix.security.RolePermissionCache
import com.froilan.synectix.security.SessionContext
import com.froilan.synectix.security.SynectixPermissionEvaluator
import com.froilan.synectix.service.AccountService
import com.froilan.synectix.service.ApiKeyService
import com.froilan.synectix.service.AuthService
import com.froilan.synectix.service.InventoryReportsService
import com.froilan.synectix.service.InventoryValuationService
import com.froilan.synectix.service.JournalEntryService
import com.froilan.synectix.util.TokenHasher
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

@WebMvcTest(controllers = [InventoryReportsController::class])
@Import(LoggingAspect::class, TestSecurityConfig::class, SynectixPermissionEvaluator::class)
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

    @BeforeEach
    fun setup() {
        setupAuthWithPermissions("inventory:read")
        `when`(authenticationContext.organizationId()).thenReturn("org-123")
    }

    private fun setupAuthWithPermissions(vararg permissions: String) {
        val roleAuthorities = testUser.roleAssignments.map { SimpleGrantedAuthority("ROLE_${it.role}") }
        val permissionAuthorities = permissions.map { SimpleGrantedAuthority(it) }
        val authentication = UsernamePasswordAuthenticationToken(testUser, null, roleAuthorities + permissionAuthorities)
        authentication.details = SessionContext(sessionId = "session-123", organizationId = "org-123")
        SecurityContextHolder.getContext().authentication = authentication
    }

    @Test
    fun `GET valuation should return 200 with report`() {
        `when`(inventoryValuationService.valuation(any())).thenReturn(
            ValuationReportResponse(
                costingMethod = InventoryCostingMethod.WEIGHTED_AVERAGE,
                lines = listOf(ValuationLineResponse("p-1", "wh-1", BigDecimal("10"), BigDecimal("50"))),
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
                        StockOnHandLineResponse("p-1", "wh-1", BigDecimal("10")),
                        StockOnHandLineResponse("p-2", "wh-1", BigDecimal("5")),
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
                lines = listOf(StockOnHandLineResponse("p-1", "wh-1", BigDecimal("10"))),
            ),
        )
        mockMvc
            .perform(
                get("/inventory/reports/stock-on-hand")
                    .param("productId", "p-1")
                    .param("warehouseId", "wh-1")
                    .param("asOfDate", "2026-05-01T00:00:00"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.lines[0].productId").value("p-1"))
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
                            id = "m-1",
                            type = StockMovementType.RECEIPT,
                            productId = "p-1",
                            warehouseId = "wh-1",
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
