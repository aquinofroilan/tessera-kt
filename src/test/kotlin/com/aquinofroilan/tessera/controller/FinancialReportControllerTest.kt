package com.aquinofroilan.tessera.controller

import com.aquinofroilan.tessera.aspect.LoggingAspect
import com.aquinofroilan.tessera.config.TestSecurityConfig
import com.aquinofroilan.tessera.dto.BalanceSheetResponse
import com.aquinofroilan.tessera.dto.ComparativeTrialBalanceResponse
import com.aquinofroilan.tessera.dto.IncomeStatementResponse
import com.aquinofroilan.tessera.dto.ReportAccountLine
import com.aquinofroilan.tessera.dto.SyntheticAccountIds
import com.aquinofroilan.tessera.dto.TrialBalanceResponse
import com.aquinofroilan.tessera.model.RoleAssignment
import com.aquinofroilan.tessera.model.User
import com.aquinofroilan.tessera.repository.ApiKeyRepository
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
import com.aquinofroilan.tessera.service.ApiKeyService
import com.aquinofroilan.tessera.service.AuthService
import com.aquinofroilan.tessera.service.FinancialReportService
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
import java.time.LocalDate
import java.util.UUID

@WebMvcTest(controllers = [FinancialReportController::class])
@Import(LoggingAspect::class, TestSecurityConfig::class, TesseraPermissionEvaluator::class)
@ActiveProfiles("test")
class FinancialReportControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var financialReportService: FinancialReportService

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
    private lateinit var apiKeyRepository: ApiKeyRepository

    @MockitoBean
    private lateinit var tokenHasher: TokenHasher

    @MockitoBean
    private lateinit var rolePermissionCache: RolePermissionCache

    @MockitoBean
    private lateinit var apiKeyService: ApiKeyService

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
        setupAuthWithPermissions("journal:read")
        `when`(authenticationContext.organizationId()).thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000124"))
        `when`(authenticationContext.userId()).thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000123"))
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
    fun `GET trial-balance returns 200 with current and comparative blocks`() {
        val current =
            TrialBalanceResponse(emptyList(), BigDecimal("500.00"), BigDecimal("500.00"), "2026-03-31")
        val compare =
            TrialBalanceResponse(emptyList(), BigDecimal("400.00"), BigDecimal("400.00"), "2026-02-28")
        `when`(financialReportService.getComparativeTrialBalance(any(), anyOrNull(), anyOrNull()))
            .thenReturn(ComparativeTrialBalanceResponse(current = current, comparative = compare))

        mockMvc
            .perform(get("/finance/reports/trial-balance?asOfDate=2026-03-31&compareAsOfDate=2026-02-28"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.current.totalDebits").value(500.00))
            .andExpect(jsonPath("$.comparative.totalDebits").value(400.00))
    }

    @Test
    fun `GET income-statement returns 200 and required fields`() {
        val response =
            IncomeStatementResponse(
                startDate = "2026-03-01",
                endDate = "2026-03-31",
                comparativePeriod = null,
                revenue =
                    listOf(
                        ReportAccountLine(UUID.fromString("00000000-0000-0000-0000-000000000999"), "4000", "Sales", BigDecimal("1000.00")),
                    ),
                totalRevenue = BigDecimal("1000.00"),
                comparativeTotalRevenue = null,
                expenses =
                    listOf(
                        ReportAccountLine(UUID.fromString("00000000-0000-0000-0000-000000000998"), "5000", "COGS", BigDecimal("300.00")),
                    ),
                totalExpenses = BigDecimal("300.00"),
                comparativeTotalExpenses = null,
                netIncome = BigDecimal("700.00"),
                comparativeNetIncome = null,
            )
        `when`(
            financialReportService.getIncomeStatement(
                eq(UUID.fromString("00000000-0000-0000-0000-000000000124")),
                eq(LocalDate.of(2026, 3, 1)),
                eq(LocalDate.of(2026, 3, 31)),
                anyOrNull(),
                anyOrNull(),
            ),
        ).thenReturn(response)

        mockMvc
            .perform(get("/finance/reports/income-statement?startDate=2026-03-01&endDate=2026-03-31"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalRevenue").value(1000.00))
            .andExpect(jsonPath("$.totalExpenses").value(300.00))
            .andExpect(jsonPath("$.netIncome").value(700.00))
    }

    @Test
    fun `GET income-statement returns 400 when required date is missing`() {
        mockMvc
            .perform(get("/finance/reports/income-statement?startDate=2026-03-01"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `GET balance-sheet returns 200 with synthetic earnings line`() {
        val response =
            BalanceSheetResponse(
                asOfDate = "2026-03-31",
                comparativeAsOfDate = null,
                assets = emptyList(),
                totalAssets = BigDecimal("1000.00"),
                comparativeTotalAssets = null,
                liabilities = emptyList(),
                totalLiabilities = BigDecimal.ZERO,
                comparativeTotalLiabilities = null,
                equity =
                    listOf(
                        ReportAccountLine(
                            accountId = SyntheticAccountIds.CURRENT_PERIOD_EARNINGS_ID,
                            accountCode = SyntheticAccountIds.CURRENT_PERIOD_EARNINGS,
                            accountName = "Current Period Earnings",
                            amount = BigDecimal("1000.00"),
                            isSynthetic = true,
                        ),
                    ),
                totalEquity = BigDecimal("1000.00"),
                comparativeTotalEquity = null,
                currentEarnings = BigDecimal("1000.00"),
                comparativeCurrentEarnings = null,
                totalLiabilitiesAndEquity = BigDecimal("1000.00"),
                comparativeTotalLiabilitiesAndEquity = null,
                isBalanced = true,
                outOfBalanceAmount = BigDecimal.ZERO,
            )
        `when`(financialReportService.getBalanceSheet(any(), any(), anyOrNull()))
            .thenReturn(response)

        mockMvc
            .perform(get("/finance/reports/balance-sheet?asOfDate=2026-03-31"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.balanced").value(true))
            .andExpect(jsonPath("$.equity[0].synthetic").value(true))
            .andExpect(jsonPath("$.equity[0].accountId").value(SyntheticAccountIds.CURRENT_PERIOD_EARNINGS))
    }

    @Test
    fun `GET trial-balance returns 403 without journal read permission`() {
        setupAuthWithPermissions("account:read")

        mockMvc
            .perform(get("/finance/reports/trial-balance"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `GET income-statement returns 403 without journal read permission`() {
        setupAuthWithPermissions("account:read")

        mockMvc
            .perform(get("/finance/reports/income-statement?startDate=2026-03-01&endDate=2026-03-31"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `GET balance-sheet returns 403 without journal read permission`() {
        setupAuthWithPermissions("account:read")

        mockMvc
            .perform(get("/finance/reports/balance-sheet?asOfDate=2026-03-31"))
            .andExpect(status().isForbidden)
    }
}
