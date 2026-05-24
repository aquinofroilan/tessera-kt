package com.froilan.synectix.controller

import com.froilan.synectix.aspect.LoggingAspect
import com.froilan.synectix.config.TestSecurityConfig
import com.froilan.synectix.model.RoleAssignment
import com.froilan.synectix.model.StockMovement
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
import com.froilan.synectix.service.JournalEntryService
import com.froilan.synectix.service.StockMovementService
import com.froilan.synectix.util.TokenHasher
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
import java.time.LocalDateTime

@WebMvcTest(controllers = [StockMovementController::class])
@Import(LoggingAspect::class, TestSecurityConfig::class, SynectixPermissionEvaluator::class)
@ActiveProfiles("test")
class StockMovementControllerTest {
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
    private lateinit var stockMovementService: StockMovementService

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
        setupAuthWithPermissions("inventory:read", "inventory:write")
        `when`(authenticationContext.organizationId()).thenReturn("org-123")
        `when`(authenticationContext.userId()).thenReturn("user-123")
    }

    private fun setupAuthWithPermissions(vararg permissions: String) {
        val roleAuthorities = testUser.roleAssignments.map { SimpleGrantedAuthority("ROLE_${it.role}") }
        val permissionAuthorities = permissions.map { SimpleGrantedAuthority(it) }
        val authentication = UsernamePasswordAuthenticationToken(testUser, null, roleAuthorities + permissionAuthorities)
        authentication.details = SessionContext(sessionId = "session-123", organizationId = "org-123")
        SecurityContextHolder.getContext().authentication = authentication
    }

    private fun mockMovement() =
        StockMovement(
            id = "mov-1",
            organizationId = "org-123",
            type = StockMovementType.RECEIPT,
            productId = "prod-1",
            warehouseId = "wh-1",
            quantity = BigDecimal("10"),
            unitCost = BigDecimal("5"),
            occurredAt = LocalDateTime.now(),
            createdBy = "user-123",
            createdAt = LocalDateTime.now(),
        )

    @Test
    fun `POST movements should return 201`() {
        `when`(stockMovementService.createMovement(any(), any(), any())).thenReturn(mockMovement())
        mockMvc
            .perform(
                post("/inventory/movements")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{
                            "type": "RECEIPT",
                            "productId": "prod-1",
                            "warehouseId": "wh-1",
                            "quantity": "10",
                            "unitCost": "5"
                        }""",
                    ),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.type").value("RECEIPT"))
    }

    @Test
    fun `POST movements should return 400 when productId blank`() {
        mockMvc
            .perform(
                post("/inventory/movements")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{
                            "type": "RECEIPT",
                            "productId": "",
                            "warehouseId": "wh-1",
                            "quantity": "10",
                            "unitCost": "5"
                        }""",
                    ),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `GET movements should return 200`() {
        `when`(stockMovementService.listMovements(any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(listOf(mockMovement()))
        mockMvc
            .perform(get("/inventory/movements"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
    }

    @Test
    fun `GET stock-on-hand should return quantity`() {
        `when`(stockMovementService.onHand(any(), any(), any())).thenReturn(BigDecimal("42"))
        mockMvc
            .perform(get("/inventory/stock-on-hand").param("productId", "prod-1").param("warehouseId", "wh-1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.productId").value("prod-1"))
            .andExpect(jsonPath("$.warehouseId").value("wh-1"))
            .andExpect(jsonPath("$.quantity").value(42))
    }

    @Test
    fun `POST movements requires inventory write`() {
        setupAuthWithPermissions("inventory:read")
        mockMvc
            .perform(
                post("/inventory/movements")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{
                            "type": "RECEIPT",
                            "productId": "prod-1",
                            "warehouseId": "wh-1",
                            "quantity": "10",
                            "unitCost": "5"
                        }""",
                    ),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `GET movements requires inventory read`() {
        setupAuthWithPermissions("inventory:write")
        mockMvc
            .perform(get("/inventory/movements"))
            .andExpect(status().isForbidden)
    }
}
