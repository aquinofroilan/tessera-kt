package com.aquinofroilan.tessera.domain.crm.controller

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
import com.aquinofroilan.tessera.domain.crm.dto.CustomerPortalSummaryResponse
import com.aquinofroilan.tessera.domain.crm.dto.PortalInvoiceResponse
import com.aquinofroilan.tessera.domain.crm.dto.PortalOrderResponse
import com.aquinofroilan.tessera.domain.crm.dto.SupportTicketResponse
import com.aquinofroilan.tessera.domain.crm.model.TicketCategory
import com.aquinofroilan.tessera.domain.crm.model.TicketPriority
import com.aquinofroilan.tessera.domain.crm.model.TicketStatus
import com.aquinofroilan.tessera.domain.crm.service.CustomerPortalService
import com.aquinofroilan.tessera.domain.finance.model.InvoiceStatus
import com.aquinofroilan.tessera.domain.finance.service.AccountService
import com.aquinofroilan.tessera.domain.finance.service.JournalEntryService
import com.aquinofroilan.tessera.domain.organization.repository.OrganizationRepository
import com.aquinofroilan.tessera.domain.sales.model.CustomerSegment
import com.aquinofroilan.tessera.domain.sales.model.SalesOrderStatus
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

@WebMvcTest(controllers = [CustomerPortalController::class, CustomerPortalAdminController::class])
@Import(WebMvcConfig::class, LoggingAspect::class, TestSecurityConfig::class, TesseraPermissionEvaluator::class)
@ActiveProfiles("test")
class CustomerPortalControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var customerPortalService: CustomerPortalService

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
    private val testCustomerId = UUID.fromString("11111111-2222-3333-4444-555555555555")

    private val testUser =
        User(
            uuid = testUserId,
            username = "portaluser",
            email = "customer@example.com",
            firstName = "Customer",
            lastName = "User",
            passwordHash = "encoded",
            organizationId = testOrgId,
            roleAssignments = listOf(RoleAssignment("CUSTOMER", testOrgId)),
        )

    @BeforeEach
    fun setup() {
        val roleAuthorities = testUser.roleAssignments.map { SimpleGrantedAuthority("ROLE_${it.role}") }
        val permissionAuthorities = listOf("crm:read", "crm:create", "sales:read", "sales:create").map { SimpleGrantedAuthority(it) }
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

    @Test
    fun `GET portal me returns summary`() {
        val summary =
            CustomerPortalSummaryResponse(
                customerId = testCustomerId,
                customerName = "Acme Corp",
                contactName = "Acme Contact",
                email = "contact@acme.com",
                phone = "123-456",
                customerSegment = CustomerSegment.RETAIL,
                openInvoicesCount = 2L,
                totalOutstandingBalance = BigDecimal("350.00"),
                activeOrdersCount = 1L,
                openTicketsCount = 1L,
            )
        `when`(customerPortalService.getMyPortalSummary(testOrgId, testUserId)).thenReturn(summary)

        mockMvc
            .perform(get("/api/v1/portal/me"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.customerId").value(testCustomerId.toString()))
            .andExpect(jsonPath("$.customerName").value("Acme Corp"))
            .andExpect(jsonPath("$.openInvoicesCount").value(2))
    }

    @Test
    fun `GET portal invoices returns list`() {
        val invoice =
            PortalInvoiceResponse(
                id = UUID.randomUUID(),
                invoiceNumber = "INV-0001",
                date = LocalDate.now(),
                dueDate = LocalDate.now().plusDays(30),
                referenceNumber = "REF-001",
                status = InvoiceStatus.APPROVED,
                currencyCode = "USD",
                totalAmount = BigDecimal("200.00"),
                amountReceived = BigDecimal.ZERO,
                balanceDue = BigDecimal("200.00"),
                lines = emptyList(),
                createdAt = LocalDateTime.now(),
            )
        `when`(customerPortalService.getMyInvoices(eq(testOrgId), eq(testUserId), anyOrNull()))
            .thenReturn(listOf(invoice))

        mockMvc
            .perform(get("/api/v1/portal/invoices"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].invoiceNumber").value("INV-0001"))
    }

    @Test
    fun `GET portal orders returns list`() {
        val order =
            PortalOrderResponse(
                id = UUID.randomUUID(),
                soNumber = "SO-0001",
                orderDate = LocalDate.now(),
                expectedDate = null,
                status = SalesOrderStatus.APPROVED,
                totalAmount = BigDecimal("500.00"),
                lines = emptyList(),
                createdAt = LocalDateTime.now(),
            )
        `when`(customerPortalService.getMyOrders(eq(testOrgId), eq(testUserId), anyOrNull()))
            .thenReturn(listOf(order))

        mockMvc
            .perform(get("/api/v1/portal/orders"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].soNumber").value("SO-0001"))
    }

    @Test
    fun `POST portal tickets creates support ticket`() {
        val ticketResponse =
            SupportTicketResponse(
                id = UUID.randomUUID(),
                organizationId = testOrgId,
                ticketNumber = 1,
                customerId = testCustomerId,
                customerName = "Acme Corp",
                contactId = null,
                subject = "Need help with order",
                description = "Status inquiry",
                status = TicketStatus.OPEN,
                priority = TicketPriority.MEDIUM,
                category = TicketCategory.ORDER_INQUIRY,
                assignedToUserId = null,
                createdByUserId = testUserId,
                resolvedAt = null,
                closedAt = null,
                messages = emptyList(),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )
        `when`(customerPortalService.createMyTicket(eq(testOrgId), eq(testUserId), any()))
            .thenReturn(ticketResponse)

        mockMvc
            .perform(
                post("/api/v1/portal/tickets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                            "subject": "Need help with order",
                            "description": "Status inquiry",
                            "category": "ORDER_INQUIRY"
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.ticketNumber").value(1))
    }
}
