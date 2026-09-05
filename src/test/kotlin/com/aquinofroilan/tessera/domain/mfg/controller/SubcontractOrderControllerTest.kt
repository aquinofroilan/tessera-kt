package com.aquinofroilan.tessera.domain.mfg.controller

import com.aquinofroilan.tessera.aspect.LoggingAspect
import com.aquinofroilan.tessera.config.OrganizationStatusInterceptor
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
import com.aquinofroilan.tessera.domain.mfg.dto.SubcontractComponentDto
import com.aquinofroilan.tessera.domain.mfg.dto.SubcontractOrderResponse
import com.aquinofroilan.tessera.domain.mfg.model.SubcontractOrderStatus
import com.aquinofroilan.tessera.domain.mfg.service.SubcontractOrderService
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
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@WebMvcTest(controllers = [SubcontractOrderController::class])
@Import(WebMvcConfig::class, LoggingAspect::class, TestSecurityConfig::class, TesseraPermissionEvaluator::class)
@ActiveProfiles("test")
class SubcontractOrderControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var subcontractOrderService: SubcontractOrderService

    @MockitoBean
    private lateinit var organizationStatusInterceptor: OrganizationStatusInterceptor

    @MockitoBean
    private lateinit var userRepository: UserRepository

    @MockitoBean
    private lateinit var sessionTokenRepository: SessionTokenRepository

    @MockitoBean
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    @MockitoBean
    private lateinit var passwordResetTokenRepository: PasswordResetTokenRepository

    @MockitoBean
    private lateinit var invitationRepository: InvitationRepository

    @MockitoBean
    private lateinit var organizationRepository: OrganizationRepository

    @MockitoBean
    private lateinit var authService: AuthService

    @MockitoBean
    private lateinit var apiKeyService: ApiKeyService

    @MockitoBean
    private lateinit var rolePermissionCache: RolePermissionCache

    @MockitoBean
    private lateinit var tokenHasher: TokenHasher

    @MockitoBean
    private lateinit var accountService: AccountService

    @MockitoBean
    private lateinit var journalEntryService: JournalEntryService

    @MockitoBean
    private lateinit var authenticationContext: AuthenticationContext

    private val testOrgId = UUID.fromString("4abe9f6d-6df3-6e5c-953e-3695db9a5216")
    private val testUserId = UUID.fromString("bc17c97c-3d89-7d43-b7e0-7ca0266eafa8")
    private val testOrderId = UUID.fromString("11111111-2222-3333-4444-555555555555")
    private val testVendorId = UUID.fromString("22222222-3333-4444-5555-666666666666")
    private val testWorkOrderId = UUID.fromString("33333333-4444-5555-6666-777777777777")
    private val testProductId = UUID.fromString("44444444-5555-6666-7777-888888888888")

    private val testUser =
        User(
            uuid = testUserId,
            username = "mfguser",
            email = "mfg@example.com",
            firstName = "Mfg",
            lastName = "Planner",
            passwordHash = "encoded",
            organizationId = testOrgId,
            roleAssignments = listOf(RoleAssignment("PRODUCTION_MANAGER", testOrgId)),
        )

    @BeforeEach
    fun setup() {
        val roleAuthorities = testUser.roleAssignments.map { SimpleGrantedAuthority("ROLE_${it.role}") }
        val permissionAuthorities =
            listOf(
                "mfg:read",
                "mfg:write",
                "manufacturing:read",
                "manufacturing:write",
            ).map { SimpleGrantedAuthority(it) }
        val authentication = UsernamePasswordAuthenticationToken(testUser, null, roleAuthorities + permissionAuthorities)
        authentication.details =
            SessionContext(
                sessionId = UUID.fromString("79c5ca4c-8e48-a8f8-6ffc-5b3271a250aa"),
                organizationId = testOrgId,
            )
        SecurityContextHolder.getContext().authentication = authentication
        `when`(authenticationContext.organizationId()).thenReturn(testOrgId)
        `when`(authenticationContext.userId()).thenReturn(testUserId)
        `when`(organizationStatusInterceptor.preHandle(any(), any(), any())).thenReturn(true)
    }

    private fun createResponse(status: SubcontractOrderStatus = SubcontractOrderStatus.DRAFT) =
        SubcontractOrderResponse(
            id = testOrderId,
            organizationId = testOrgId,
            orderNumber = "SCO-00001",
            workOrderId = testWorkOrderId,
            operationId = UUID.randomUUID(),
            operationNumber = 10,
            vendorId = testVendorId,
            vendorName = "Precision Plating Inc",
            purchaseOrderId = null,
            serviceItemName = "Heat Treatment Service",
            quantity = BigDecimal("100"),
            receivedQuantity = BigDecimal.ZERO,
            unitServiceCost = BigDecimal("5.00"),
            totalCost = BigDecimal("500.00"),
            status = status,
            dispatchedAt = null,
            receivedAt = null,
            completedAt = null,
            cancelledAt = null,
            notes = "Test notes",
            components =
                listOf(
                    SubcontractComponentDto(
                        id = UUID.randomUUID(),
                        productId = testProductId,
                        productSku = "RAW-STEEL",
                        productName = "Raw Steel Plate",
                        plannedQuantity = BigDecimal("100"),
                        dispatchedQuantity = BigDecimal.ZERO,
                        uom = "PCS",
                    ),
                ),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

    @Test
    fun `GET subcontract-orders returns list of orders`() {
        `when`(subcontractOrderService.listSubcontractOrders(eq(testOrgId), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(listOf(createResponse()))

        mockMvc
            .perform(get("/api/v1/mfg/subcontract-orders"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(testOrderId.toString()))
            .andExpect(jsonPath("$[0].orderNumber").value("SCO-00001"))
            .andExpect(jsonPath("$[0].vendorName").value("Precision Plating Inc"))
    }

    @Test
    fun `GET subcontract-orders by id returns order details`() {
        `when`(subcontractOrderService.getSubcontractOrder(testOrderId, testOrgId))
            .thenReturn(createResponse())

        mockMvc
            .perform(get("/api/v1/mfg/subcontract-orders/$testOrderId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(testOrderId.toString()))
            .andExpect(jsonPath("$.serviceItemName").value("Heat Treatment Service"))
    }

    @Test
    fun `POST subcontract-orders creates order and returns 201`() {
        `when`(subcontractOrderService.createSubcontractOrder(eq(testOrgId), eq(testUserId), any()))
            .thenReturn(createResponse())

        mockMvc
            .perform(
                post("/api/v1/mfg/subcontract-orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                            "workOrderId": "$testWorkOrderId",
                            "operationNumber": 10,
                            "vendorId": "$testVendorId",
                            "serviceItemName": "Heat Treatment Service",
                            "quantity": 100,
                            "unitServiceCost": 5.00
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(testOrderId.toString()))
            .andExpect(jsonPath("$.status").value("DRAFT"))
    }

    @Test
    fun `POST dispatch dispatches components and returns 200`() {
        val dispatched = createResponse(SubcontractOrderStatus.DISPATCHED).copy(dispatchedAt = LocalDateTime.now())
        `when`(subcontractOrderService.dispatchComponents(eq(testOrderId), eq(testOrgId), eq(testUserId), any()))
            .thenReturn(dispatched)

        mockMvc
            .perform(
                post("/api/v1/mfg/subcontract-orders/$testOrderId/dispatch")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("DISPATCHED"))
    }

    @Test
    fun `POST receive receives goods and returns 200`() {
        val completed =
            createResponse(SubcontractOrderStatus.COMPLETED).copy(
                receivedQuantity = BigDecimal("100"),
                completedAt = LocalDateTime.now(),
            )
        `when`(subcontractOrderService.receiveProcessedGoods(eq(testOrderId), eq(testOrgId), eq(testUserId), any()))
            .thenReturn(completed)

        mockMvc
            .perform(
                post("/api/v1/mfg/subcontract-orders/$testOrderId/receive")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                            "quantityReceived": 100
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.receivedQuantity").value(100))
    }

    @Test
    fun `POST cancel cancels order and returns 200`() {
        val cancelled = createResponse(SubcontractOrderStatus.CANCELLED).copy(cancelledAt = LocalDateTime.now())
        `when`(subcontractOrderService.cancelSubcontractOrder(testOrderId, testOrgId, testUserId))
            .thenReturn(cancelled)

        mockMvc
            .perform(post("/api/v1/mfg/subcontract-orders/$testOrderId/cancel"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("CANCELLED"))
    }
}
