package com.aquinofroilan.tessera.controller

import com.aquinofroilan.tessera.aspect.LoggingAspect
import com.aquinofroilan.tessera.config.TestSecurityConfig
import com.aquinofroilan.tessera.model.RoleAssignment
import com.aquinofroilan.tessera.model.StockMovement
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
import com.aquinofroilan.tessera.service.JournalEntryService
import com.aquinofroilan.tessera.service.StockMovementService
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
import java.time.LocalDateTime
import java.util.UUID

@WebMvcTest(controllers = [StockMovementController::class])
@Import(LoggingAspect::class, TestSecurityConfig::class, TesseraPermissionEvaluator::class)
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
        setupAuthWithPermissions("inventory:read", "inventory:write")
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

    private fun mockMovement() =
        StockMovement(
            id = UUID.fromString("00000000-0000-0000-0000-000000000199"),
            organizationId = UUID.fromString("00000000-0000-0000-0000-000000000199"),
            type = StockMovementType.RECEIPT,
            productId = UUID.fromString("00000000-0000-0000-0000-000000000999"),
            warehouseId = UUID.fromString("00000000-0000-0000-0000-000000000999"),
            quantity = BigDecimal("10"),
            unitCost = BigDecimal("5"),
            occurredAt = LocalDateTime.now(),
            createdBy = UUID.fromString("00000000-0000-0000-0000-000000000199"),
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
                            "productId": "00000000-0000-0000-0000-000000000199",
                            "warehouseId": "00000000-0000-0000-0000-000000000199",
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
                            "warehouseId": "00000000-0000-0000-0000-000000000199",
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
            .perform(
                get("/inventory/stock-on-hand")
                    .param("productId", "00000000-0000-0000-0000-000000000199")
                    .param("warehouseId", "00000000-0000-0000-0000-000000000199"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.productId").value("00000000-0000-0000-0000-000000000199"))
            .andExpect(jsonPath("$.warehouseId").value("00000000-0000-0000-0000-000000000199"))
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
                            "productId": "00000000-0000-0000-0000-000000000199",
                            "warehouseId": "00000000-0000-0000-0000-000000000199",
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
