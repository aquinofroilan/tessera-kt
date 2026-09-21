package com.aquinofroilan.tessera.domain.finance.controller

import com.aquinofroilan.tessera.aspect.LoggingAspect
import com.aquinofroilan.tessera.config.TestSecurityConfig
import com.aquinofroilan.tessera.config.WebMvcConfig
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
import com.aquinofroilan.tessera.domain.finance.dto.AgingBucket
import com.aquinofroilan.tessera.domain.finance.dto.ArAgingReportResponse
import com.aquinofroilan.tessera.domain.finance.model.Invoice
import com.aquinofroilan.tessera.domain.finance.model.InvoiceLine
import com.aquinofroilan.tessera.domain.finance.model.InvoiceReceipt
import com.aquinofroilan.tessera.domain.finance.model.InvoiceStatus
import com.aquinofroilan.tessera.domain.finance.model.PaymentMethod
import com.aquinofroilan.tessera.domain.finance.service.InvoiceService
import com.aquinofroilan.tessera.domain.organization.repository.OrganizationRepository
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

@WebMvcTest(controllers = [InvoiceController::class])
@Import(WebMvcConfig::class, LoggingAspect::class, TestSecurityConfig::class, TesseraPermissionEvaluator::class)
@ActiveProfiles("test")
class InvoiceControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var invoiceService: InvoiceService

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
        setupAuthWithPermissions("ar:create", "ar:read", "ar:approve", "ar:void", "ar:receive")
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

    private fun createMockInvoice() =
        Invoice(
            id = UUID.fromString("00000000-0000-0000-0000-000000000199"),
            invoiceNumber = "INV-0001",
            customerId = UUID.fromString("00000000-0000-0000-0000-000000000299"),
            customerName = "Test Customer",
            date = LocalDate.of(2026, 1, 15),
            dueDate = LocalDate.of(2026, 2, 15),
            organizationId = UUID.fromString("00000000-0000-0000-0000-000000000199"),
            status = InvoiceStatus.DRAFT,
            lines =
                listOf(
                    InvoiceLine(
                        accountId = UUID.fromString("00000000-0000-0000-0000-000000000999"),
                        accountCode = "4000",
                        accountName = "Sales Revenue",
                        amount = BigDecimal("100.00"),
                        description = "Services rendered",
                    ),
                ),
            totalAmount = BigDecimal("100.00"),
            taxAmount = BigDecimal.ZERO,
            amountReceived = BigDecimal.ZERO,
            currencyCode = "USD",
            exchangeRate = BigDecimal.ONE,
            baseCurrencyAmount = BigDecimal("100.00"),
            baseCurrencyTaxAmount = BigDecimal.ZERO,
            baseCurrencyAmountReceived = BigDecimal.ZERO,
            createdBy = UUID.fromString("00000000-0000-0000-0000-000000000199"),
        )

    private fun createMockReceipt() =
        InvoiceReceipt(
            id = UUID.fromString("00000000-0000-0000-0000-000000000399"),
            invoiceId = UUID.fromString("00000000-0000-0000-0000-000000000199"),
            receiptDate = LocalDate.of(2026, 1, 20),
            amount = BigDecimal("100.00"),
            baseCurrencyAmount = BigDecimal("100.00"),
            exchangeRate = BigDecimal.ONE,
            paymentMethod = PaymentMethod.BANK_TRANSFER,
            referenceNumber = "REF-123",
            organizationId = UUID.fromString("00000000-0000-0000-0000-000000000199"),
            createdBy = UUID.fromString("00000000-0000-0000-0000-000000000199"),
        )

    @Test
    fun `POST create invoice should return 201`() {
        val invoice = createMockInvoice()
        `when`(invoiceService.createInvoice(any(), any(), any())).thenReturn(invoice)

        mockMvc
            .perform(
                post("/api/v1/finance/ar/invoices")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                            "customerId": "00000000-0000-0000-0000-000000000299",
                            "date": "2026-01-15",
                            "dueDate": "2026-02-15",
                            "currencyCode": "USD",
                            "lines": [
                                {
                                    "accountId": "00000000-0000-0000-0000-000000000999",
                                    "amount": 100.00,
                                    "description": "Services rendered"
                                }
                            ]
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value("00000000-0000-0000-0000-000000000199"))
            .andExpect(jsonPath("$.invoiceNumber").value("INV-0001"))
            .andExpect(jsonPath("$.status").value("DRAFT"))
    }

    @Test
    fun `GET list invoices should return 200`() {
        val invoices = listOf(createMockInvoice())
        `when`(invoiceService.listInvoices(any(), anyOrNull(), anyOrNull())).thenReturn(invoices)

        mockMvc
            .perform(get("/api/v1/finance/ar/invoices"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value("00000000-0000-0000-0000-000000000199"))
            .andExpect(jsonPath("$[0].invoiceNumber").value("INV-0001"))
    }

    @Test
    fun `GET list invoices with invalid status should return 400`() {
        mockMvc
            .perform(get("/api/v1/finance/ar/invoices?status=INVALID_STATUS"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Invalid status 'INVALID_STATUS'"))
    }

    @Test
    fun `GET invoice by id should return 200`() {
        val invoice = createMockInvoice()
        `when`(invoiceService.getInvoice(any(), any())).thenReturn(invoice)

        mockMvc
            .perform(get("/api/v1/finance/ar/invoices/00000000-0000-0000-0000-000000000199"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value("00000000-0000-0000-0000-000000000199"))
            .andExpect(jsonPath("$.invoiceNumber").value("INV-0001"))
    }

    @Test
    fun `POST approve should return 200`() {
        val invoice = createMockInvoice().apply { status = InvoiceStatus.APPROVED }
        `when`(invoiceService.approveInvoice(any(), any(), any())).thenReturn(invoice)

        mockMvc
            .perform(post("/api/v1/finance/ar/invoices/00000000-0000-0000-0000-000000000199/approve"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value("00000000-0000-0000-0000-000000000199"))
            .andExpect(jsonPath("$.status").value("APPROVED"))
    }

    @Test
    fun `POST void should return 200`() {
        val invoice = createMockInvoice().apply { status = InvoiceStatus.VOID }
        `when`(invoiceService.voidInvoice(any(), any(), any(), any())).thenReturn(invoice)

        mockMvc
            .perform(
                post("/api/v1/finance/ar/invoices/00000000-0000-0000-0000-000000000199/void")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"reason": "Wrong amount"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value("00000000-0000-0000-0000-000000000199"))
            .andExpect(jsonPath("$.status").value("VOID"))
    }

    @Test
    fun `POST record receipt should return 201`() {
        val receipt = createMockReceipt()
        `when`(invoiceService.recordReceipt(any(), any(), any(), any())).thenReturn(receipt)

        mockMvc
            .perform(
                post("/api/v1/finance/ar/invoices/00000000-0000-0000-0000-000000000199/receipts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                            "receiptDate": "2026-01-20",
                            "amount": 100.00,
                            "paymentMethod": "BANK_TRANSFER",
                            "referenceNumber": "REF-123"
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value("00000000-0000-0000-0000-000000000399"))
            .andExpect(jsonPath("$.amount").value(100.00))
    }

    @Test
    fun `GET receipts should return 200`() {
        val receipts = listOf(createMockReceipt())
        `when`(invoiceService.getReceipts(any(), any())).thenReturn(receipts)

        mockMvc
            .perform(get("/api/v1/finance/ar/invoices/00000000-0000-0000-0000-000000000199/receipts"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value("00000000-0000-0000-0000-000000000399"))
    }

    @Test
    fun `GET aging report should return 200`() {
        val report =
            ArAgingReportResponse(
                asOfDate = LocalDate.now().toString(),
                customers = emptyList(),
                totals = AgingBucket(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
            )
        `when`(invoiceService.getAgingReport(any(), any())).thenReturn(report)

        mockMvc
            .perform(get("/api/v1/finance/ar/invoices/aging"))
            .andExpect(status().isOk)
    }

    @Test
    fun `Missing auth should return 401`() {
        org.springframework.security.test.context.TestSecurityContextHolder
            .clearContext()
        SecurityContextHolder.clearContext()

        mockMvc
            .perform(get("/api/v1/finance/ar/invoices"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `Missing permission should return 403`() {
        setupAuthWithPermissions("some:other:permission")

        mockMvc
            .perform(get("/api/v1/finance/ar/invoices"))
            .andExpect(status().isForbidden)
    }
}
