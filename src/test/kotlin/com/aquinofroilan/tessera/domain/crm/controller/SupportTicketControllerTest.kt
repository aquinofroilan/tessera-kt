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
import com.aquinofroilan.tessera.domain.crm.dto.SupportTicketMessageDto
import com.aquinofroilan.tessera.domain.crm.dto.SupportTicketResponse
import com.aquinofroilan.tessera.domain.crm.model.TicketCategory
import com.aquinofroilan.tessera.domain.crm.model.TicketPriority
import com.aquinofroilan.tessera.domain.crm.model.TicketSenderType
import com.aquinofroilan.tessera.domain.crm.model.TicketStatus
import com.aquinofroilan.tessera.domain.crm.service.SupportTicketService
import com.aquinofroilan.tessera.domain.finance.service.AccountService
import com.aquinofroilan.tessera.domain.finance.service.JournalEntryService
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
import java.time.LocalDateTime
import java.util.UUID

@WebMvcTest(controllers = [SupportTicketController::class])
@Import(WebMvcConfig::class, LoggingAspect::class, TestSecurityConfig::class, TesseraPermissionEvaluator::class)
@ActiveProfiles("test")
class SupportTicketControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var supportTicketService: SupportTicketService

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
    private val testTicketId = UUID.fromString("22222222-3333-4444-5555-666666666666")

    private val testUser =
        User(
            uuid = testUserId,
            username = "supportagent",
            email = "agent@example.com",
            firstName = "Support",
            lastName = "Agent",
            passwordHash = "encoded",
            organizationId = testOrgId,
            roleAssignments = listOf(RoleAssignment("CRM_MANAGER", testOrgId)),
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

    private fun createResponse() =
        SupportTicketResponse(
            id = testTicketId,
            organizationId = testOrgId,
            ticketNumber = "TICK-00001",
            customerId = testCustomerId,
            customerName = "Acme Corp",
            contactId = null,
            subject = "Issue with delivery",
            description = "Order not received",
            status = TicketStatus.OPEN,
            priority = TicketPriority.HIGH,
            category = TicketCategory.ORDER_INQUIRY,
            assignedToUserId = null,
            createdByUserId = testUserId,
            resolvedAt = null,
            closedAt = null,
            messages =
                listOf(
                    SupportTicketMessageDto(
                        id = UUID.randomUUID(),
                        ticketId = testTicketId,
                        senderId = testUserId,
                        senderType = TicketSenderType.CUSTOMER,
                        message = "Order not received",
                        isInternalNote = false,
                        createdAt = LocalDateTime.now(),
                    ),
                ),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

    @Test
    fun `GET tickets should return 200 with list`() {
        `when`(supportTicketService.listTickets(eq(testOrgId), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(listOf(createResponse()))

        mockMvc
            .perform(get("/api/v1/crm/tickets"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(testTicketId.toString()))
            .andExpect(jsonPath("$[0].ticketNumber").value("TICK-00001"))
    }

    @Test
    fun `POST ticket should return 201`() {
        `when`(supportTicketService.createTicket(eq(testOrgId), eq(testUserId), any(), any()))
            .thenReturn(createResponse())

        mockMvc
            .perform(
                post("/api/v1/crm/tickets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                            "customerId": "$testCustomerId",
                            "subject": "Issue with delivery",
                            "description": "Order not received",
                            "priority": "HIGH",
                            "category": "ORDER_INQUIRY"
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(testTicketId.toString()))
    }

    @Test
    fun `POST add message should return 200`() {
        `when`(supportTicketService.addMessage(eq(testTicketId), eq(testOrgId), eq(testUserId), eq(TicketSenderType.AGENT), any()))
            .thenReturn(createResponse())

        mockMvc
            .perform(
                post("/api/v1/crm/tickets/$testTicketId/messages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                            "message": "Tracking details sent"
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(testTicketId.toString()))
    }

    @Test
    fun `POST resolve should return 200`() {
        val resolved = createResponse().copy(status = TicketStatus.RESOLVED)
        `when`(supportTicketService.resolveTicket(testTicketId, testOrgId)).thenReturn(resolved)

        mockMvc
            .perform(post("/api/v1/crm/tickets/$testTicketId/resolve"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("RESOLVED"))
    }
}
