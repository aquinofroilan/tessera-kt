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
import com.aquinofroilan.tessera.domain.sales.dto.CreditNoteLineDto
import com.aquinofroilan.tessera.domain.sales.dto.CreditNoteResponse
import com.aquinofroilan.tessera.domain.sales.model.CreditNoteStatus
import com.aquinofroilan.tessera.domain.sales.service.CreditNoteService
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@WebMvcTest(controllers = [CreditNoteController::class])
@Import(WebMvcConfig::class, LoggingAspect::class, TestSecurityConfig::class, TesseraPermissionEvaluator::class)
@ActiveProfiles("test")
class CreditNoteControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var creditNoteService: CreditNoteService

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
    private val testCreditNoteId = UUID.fromString("22222222-3333-4444-5555-666666666666")
    private val testCustomerId = UUID.fromString("11111111-2222-3333-4444-555555555555")
    private val testInvoiceId = UUID.fromString("33333333-4444-5555-6666-777777777777")

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
        val permissionAuthorities = listOf("sales:read", "sales:create", "ar:read", "ar:create").map { SimpleGrantedAuthority(it) }
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

    private fun createResponse() =
        CreditNoteResponse(
            id = testCreditNoteId,
            organizationId = testOrgId,
            creditNoteNumber = "CN-00001",
            customerId = testCustomerId,
            customerName = "Acme Corp",
            salesReturnId = null,
            invoiceId = null,
            date = LocalDate.now(),
            currency = "USD",
            totalAmount = BigDecimal("100.00"),
            allocatedAmount = BigDecimal.ZERO,
            unallocatedAmount = BigDecimal("100.00"),
            status = CreditNoteStatus.DRAFT,
            reason = "Goodwill",
            createdBy = testUserId,
            approvedBy = null,
            approvedAt = null,
            lines =
                listOf(
                    CreditNoteLineDto(
                        id = UUID.randomUUID(),
                        lineNumber = 1,
                        productId = null,
                        description = "Credit for return",
                        quantity = BigDecimal.ONE,
                        unitPrice = BigDecimal("100.00"),
                        lineTotal = BigDecimal("100.00"),
                        accountId = null,
                    ),
                ),
            allocations = emptyList(),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

    @Test
    fun `GET credit notes should return 200 with list`() {
        `when`(creditNoteService.listCreditNotes(eq(testOrgId), anyOrNull(), anyOrNull()))
            .thenReturn(listOf(createResponse()))

        mockMvc
            .perform(get("/api/v1/sales/credit-notes"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(testCreditNoteId.toString()))
            .andExpect(jsonPath("$[0].creditNoteNumber").value("CN-00001"))
    }

    @Test
    fun `POST credit note should return 201`() {
        `when`(creditNoteService.createCreditNote(eq(testOrgId), eq(testUserId), any()))
            .thenReturn(createResponse())

        mockMvc
            .perform(
                post("/api/v1/sales/credit-notes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                            "customerId": "$testCustomerId",
                            "reason": "Goodwill",
                            "lines": [
                                {
                                    "description": "Credit for return",
                                    "unitPrice": 100.0
                                }
                            ]
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(testCreditNoteId.toString()))
    }

    @Test
    fun `POST approve credit note should return 200`() {
        val approved = createResponse().copy(status = CreditNoteStatus.APPROVED)
        `when`(creditNoteService.approveCreditNote(testCreditNoteId, testOrgId, testUserId)).thenReturn(approved)

        mockMvc
            .perform(post("/api/v1/sales/credit-notes/$testCreditNoteId/approve"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("APPROVED"))
    }

    @Test
    fun `POST apply credit note should return 200`() {
        val applied =
            createResponse().copy(
                status = CreditNoteStatus.APPLIED,
                allocatedAmount = BigDecimal("100.00"),
                unallocatedAmount = BigDecimal.ZERO,
            )
        `when`(creditNoteService.applyCreditNoteToInvoice(eq(testCreditNoteId), eq(testOrgId), eq(testUserId), any()))
            .thenReturn(applied)

        mockMvc
            .perform(
                post("/api/v1/sales/credit-notes/$testCreditNoteId/apply")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                            "invoiceId": "$testInvoiceId",
                            "amount": 100.0
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("APPLIED"))
            .andExpect(jsonPath("$.allocatedAmount").value(100.0))
    }
}
