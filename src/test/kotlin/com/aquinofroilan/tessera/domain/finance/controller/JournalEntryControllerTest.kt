package com.aquinofroilan.tessera.domain.finance.controller

import com.aquinofroilan.tessera.aspect.LoggingAspect
import com.aquinofroilan.tessera.config.TestSecurityConfig
import com.aquinofroilan.tessera.domain.auth.model.RoleAssignment
import com.aquinofroilan.tessera.domain.auth.model.User
import com.aquinofroilan.tessera.domain.auth.repository.ApiKeyRepository
import com.aquinofroilan.tessera.domain.auth.repository.InvitationRepository
import com.aquinofroilan.tessera.domain.auth.repository.PasswordResetTokenRepository
import com.aquinofroilan.tessera.domain.auth.repository.RefreshTokenRepository
import com.aquinofroilan.tessera.domain.auth.repository.SessionTokenRepository
import com.aquinofroilan.tessera.domain.auth.repository.UserRepository
import com.aquinofroilan.tessera.domain.auth.service.ApiKeyService
import com.aquinofroilan.tessera.domain.auth.service.AuthService
import com.aquinofroilan.tessera.domain.finance.dto.AccountBalanceResponse
import com.aquinofroilan.tessera.domain.finance.dto.TrialBalanceResponse
import com.aquinofroilan.tessera.domain.finance.model.JournalEntry
import com.aquinofroilan.tessera.domain.finance.model.JournalEntryLine
import com.aquinofroilan.tessera.domain.finance.model.JournalEntrySource
import com.aquinofroilan.tessera.domain.finance.model.JournalEntryStatus
import com.aquinofroilan.tessera.domain.finance.service.AccountService
import com.aquinofroilan.tessera.domain.finance.service.JournalEntryService
import com.aquinofroilan.tessera.domain.organization.repository.OrganizationRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.security.RolePermissionCache
import com.aquinofroilan.tessera.security.SessionContext
import com.aquinofroilan.tessera.security.TesseraPermissionEvaluator
import com.aquinofroilan.tessera.util.TokenHasher
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
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
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@WebMvcTest(controllers = [JournalEntryController::class])
@Import(LoggingAspect::class, TestSecurityConfig::class, TesseraPermissionEvaluator::class)
@ActiveProfiles("test")
class JournalEntryControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var journalEntryService: JournalEntryService

    @MockitoBean
    private lateinit var authService: AuthService

    @MockitoBean
    private lateinit var accountService: AccountService

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
            uuid = UUID.fromString("00000000-0000-0000-0000-000000000199"),
            username = "testuser",
            email = "test@example.com",
            firstName = "Test",
            lastName = "User",
            passwordHash = "encoded",
            organizationId = UUID.fromString("00000000-0000-0000-0000-000000000199"),
            roleAssignments = listOf(RoleAssignment("OWNER", UUID.fromString("00000000-0000-0000-0000-000000000199"))),
        )

    @BeforeEach
    fun setup() {
        setupAuthWithPermissions("journal:create", "journal:read", "journal:post", "journal:void", "account:read")
        `when`(authenticationContext.organizationId()).thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000199"))
        `when`(authenticationContext.userId()).thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000199"))
    }

    private fun setupAuthWithPermissions(vararg permissions: String) {
        val roleAuthorities = testUser.roleAssignments.map { SimpleGrantedAuthority("ROLE_${it.role}") }
        val permissionAuthorities = permissions.map { SimpleGrantedAuthority(it) }
        val authentication = UsernamePasswordAuthenticationToken(testUser, null, roleAuthorities + permissionAuthorities)
        authentication.details =
            SessionContext(
                sessionId = UUID.fromString("00000000-0000-0000-0000-000000000199"),
                organizationId = UUID.fromString("00000000-0000-0000-0000-000000000199"),
            )
        SecurityContextHolder.getContext().authentication = authentication
    }

    private fun createMockJournalEntry() =
        JournalEntry(
            id = UUID.fromString("00000000-0000-0000-0000-000000000199"),
            entryNumber = "JE-0001",
            date = LocalDate.of(2026, 1, 15),
            description = "Test entry",
            organizationId = UUID.fromString("00000000-0000-0000-0000-000000000199"),
            status = JournalEntryStatus.DRAFT,
            source = JournalEntrySource.MANUAL,
            lines =
                listOf(
                    JournalEntryLine(
                        accountId = UUID.fromString("00000000-0000-0000-0000-000000000999"),
                        accountCode = "1000",
                        accountName = "Cash",
                        debit = BigDecimal("100.00"),
                        credit = BigDecimal.ZERO,
                    ),
                    JournalEntryLine(
                        accountId = UUID.fromString("00000000-0000-0000-0000-000000000999"),
                        accountCode = "4000",
                        accountName = "Sales Revenue",
                        debit = BigDecimal.ZERO,
                        credit = BigDecimal("100.00"),
                    ),
                ),
            createdBy = UUID.fromString("00000000-0000-0000-0000-000000000199"),
        )

    @Test
    fun `POST journal-entries should return 201 when created`() {
        val entry = createMockJournalEntry()
        `when`(journalEntryService.createJournalEntry(any(), any(), any())).thenReturn(entry)

        mockMvc
            .perform(
                post("/api/v1/finance/journal")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                            "date": "2026-01-15",
                            "description": "Test entry",
                            "lines": [
                                {"accountId": "00000000-0000-0000-0000-000000000999", "debit": 100.00, "credit": 0},
                                {"accountId": "00000000-0000-0000-0000-000000000999", "debit": 0, "credit": 100.00}
                            ]
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value("00000000-0000-0000-0000-000000000199"))
            .andExpect(jsonPath("$.entryNumber").value("JE-0001"))
            .andExpect(jsonPath("$.date").value("2026-01-15"))
            .andExpect(jsonPath("$.description").value("Test entry"))
            .andExpect(jsonPath("$.organizationId").value("00000000-0000-0000-0000-000000000199"))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.source").value("MANUAL"))
            .andExpect(jsonPath("$.lines.length()").value(2))
            .andExpect(jsonPath("$.createdBy").value("00000000-0000-0000-0000-000000000199"))
    }

    @Test
    fun `POST journal-entries should return 400 when unbalanced`() {
        `when`(journalEntryService.createJournalEntry(any(), any(), any()))
            .thenThrow(BusinessRuleException("Journal entry must balance: debits (100.00) != credits (50.00)"))

        mockMvc
            .perform(
                post("/api/v1/finance/journal")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                            "date": "2026-01-15",
                            "description": "Unbalanced entry",
                            "lines": [
                                {"accountId": "00000000-0000-0000-0000-000000000999", "debit": 100.00, "credit": 0},
                                {"accountId": "00000000-0000-0000-0000-000000000999", "debit": 0, "credit": 50.00}
                            ]
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Journal entry must balance: debits (100.00) != credits (50.00)"))
    }

    @Test
    fun `GET journal-entries should return 200 with entry list`() {
        val entries = listOf(createMockJournalEntry())
        `when`(journalEntryService.listJournalEntries(any(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(entries)

        mockMvc
            .perform(get("/api/v1/finance/journal"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value("00000000-0000-0000-0000-000000000199"))
            .andExpect(jsonPath("$[0].entryNumber").value("JE-0001"))
            .andExpect(jsonPath("$[0].status").value("DRAFT"))
            .andExpect(jsonPath("$[0].lines.length()").value(2))
    }

    @Test
    fun `GET journal-entries by id should return 200`() {
        val entry = createMockJournalEntry()
        `when`(journalEntryService.getJournalEntry(any(), any())).thenReturn(entry)

        mockMvc
            .perform(get("/api/v1/finance/journal/00000000-0000-0000-0000-000000000199"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value("00000000-0000-0000-0000-000000000199"))
            .andExpect(jsonPath("$.entryNumber").value("JE-0001"))
            .andExpect(jsonPath("$.date").value("2026-01-15"))
            .andExpect(jsonPath("$.description").value("Test entry"))
            .andExpect(jsonPath("$.status").value("DRAFT"))
    }

    @Test
    fun `GET journal-entries by id should return 404 when not found`() {
        `when`(journalEntryService.getJournalEntry(any(), any()))
            .thenThrow(ResourceNotFoundException("Journal entry not found"))

        mockMvc
            .perform(get("/api/v1/finance/journal/00000000-0000-0000-0000-000000000000"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error").value("Journal entry not found"))
    }

    @Test
    fun `POST journal-entries id post should return 200`() {
        val entry = createMockJournalEntry().apply { status = JournalEntryStatus.POSTED }
        `when`(journalEntryService.postJournalEntry(any(), any())).thenReturn(entry)

        mockMvc
            .perform(post("/api/v1/finance/journal/00000000-0000-0000-0000-000000000199/post"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value("00000000-0000-0000-0000-000000000199"))
            .andExpect(jsonPath("$.status").value("POSTED"))
    }

    @Test
    fun `POST journal-entries id void should return 200`() {
        val entry = createMockJournalEntry().apply { status = JournalEntryStatus.VOIDED }
        `when`(journalEntryService.voidJournalEntry(any(), any(), any())).thenReturn(entry)

        mockMvc
            .perform(
                post("/api/v1/finance/journal/00000000-0000-0000-0000-000000000199/void")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"reason": "Error correction"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value("00000000-0000-0000-0000-000000000199"))
            .andExpect(jsonPath("$.status").value("VOIDED"))
    }

    @Test
    fun `GET trial-balance should return 200`() {
        val trialBalance =
            TrialBalanceResponse(
                accounts =
                    listOf(
                        AccountBalanceResponse(
                            accountId = UUID.fromString("00000000-0000-0000-0000-000000000999"),
                            accountCode = "1000",
                            accountName = "Cash",
                            accountType = "ASSET",
                            totalDebits = BigDecimal("100.00"),
                            totalCredits = BigDecimal.ZERO,
                            balance = BigDecimal("100.00"),
                        ),
                        AccountBalanceResponse(
                            accountId = UUID.fromString("00000000-0000-0000-0000-000000000999"),
                            accountCode = "4000",
                            accountName = "Sales Revenue",
                            accountType = "REVENUE",
                            totalDebits = BigDecimal.ZERO,
                            totalCredits = BigDecimal("100.00"),
                            balance = BigDecimal("100.00"),
                        ),
                    ),
                totalDebits = BigDecimal("100.00"),
                totalCredits = BigDecimal("100.00"),
                asOfDate = null,
            )
        `when`(journalEntryService.getTrialBalance(any(), anyOrNull())).thenReturn(trialBalance)

        val result =
            mockMvc
                .perform(get("/api/v1/finance/journal/trial-balance"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.accounts.length()").value(2))
                .andExpect(jsonPath("$.accounts[0].accountCode").value("1000"))
                .andExpect(jsonPath("$.accounts[1].accountCode").value("4000"))
                .andExpect(jsonPath("$.totalDebits").value(100.00))
                .andExpect(jsonPath("$.totalCredits").value(100.00))
                .andReturn()

        assertThat(result.response.status).isEqualTo(200)
    }

    @Test
    fun `POST journal-entries should return 403 without journal create permission`() {
        setupAuthWithPermissions("journal:read")

        mockMvc
            .perform(
                post("/api/v1/finance/journal")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                            "date": "2026-01-15",
                            "description": "Test entry",
                            "lines": [
                                {"accountId": "00000000-0000-0000-0000-000000000999", "debit": 100.00, "credit": 0},
                                {"accountId": "00000000-0000-0000-0000-000000000999", "debit": 0, "credit": 100.00}
                            ]
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isForbidden)
    }
}
