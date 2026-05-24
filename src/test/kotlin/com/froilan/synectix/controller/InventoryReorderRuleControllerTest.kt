package com.froilan.synectix.controller

import com.froilan.synectix.aspect.LoggingAspect
import com.froilan.synectix.config.TestSecurityConfig
import com.froilan.synectix.model.InventoryReorderRule
import com.froilan.synectix.model.RoleAssignment
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
import com.froilan.synectix.service.InventoryReorderRuleService
import com.froilan.synectix.service.JournalEntryService
import com.froilan.synectix.util.TokenHasher
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal

@WebMvcTest(controllers = [InventoryReorderRuleController::class])
@Import(LoggingAspect::class, TestSecurityConfig::class, SynectixPermissionEvaluator::class)
@ActiveProfiles("test")
class InventoryReorderRuleControllerTest {
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
    private lateinit var reorderRuleService: InventoryReorderRuleService

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
    }

    private fun setupAuthWithPermissions(vararg permissions: String) {
        val roleAuthorities = testUser.roleAssignments.map { SimpleGrantedAuthority("ROLE_${it.role}") }
        val permissionAuthorities = permissions.map { SimpleGrantedAuthority(it) }
        val authentication = UsernamePasswordAuthenticationToken(testUser, null, roleAuthorities + permissionAuthorities)
        authentication.details = SessionContext(sessionId = "session-123", organizationId = "org-123")
        SecurityContextHolder.getContext().authentication = authentication
    }

    private fun mockRule() =
        InventoryReorderRule(
            id = "rr-1",
            organizationId = "org-123",
            productId = "p-1",
            warehouseId = "wh-1",
            reorderPoint = BigDecimal("10"),
            safetyStock = BigDecimal("2"),
        )

    @Test
    fun `POST reorder-rules returns 201`() {
        `when`(reorderRuleService.createRule(any(), any())).thenReturn(mockRule())
        mockMvc
            .perform(
                post("/inventory/reorder-rules")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{
                            "productId": "p-1",
                            "warehouseId": "wh-1",
                            "reorderPoint": "10",
                            "safetyStock": "2"
                        }""",
                    ),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.productId").value("p-1"))
    }

    @Test
    fun `POST reorder-rules returns 400 when reorderPoint missing`() {
        mockMvc
            .perform(
                post("/inventory/reorder-rules")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{
                            "productId": "p-1",
                            "warehouseId": "wh-1"
                        }""",
                    ),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `GET reorder-rules returns 200 with list`() {
        `when`(reorderRuleService.listRules(any())).thenReturn(listOf(mockRule()))
        mockMvc
            .perform(get("/inventory/reorder-rules"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
    }

    @Test
    fun `DELETE reorder-rules returns 204`() {
        mockMvc
            .perform(delete("/inventory/reorder-rules/rr-1"))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `POST reorder-rules requires inventory write`() {
        setupAuthWithPermissions("inventory:read")
        mockMvc
            .perform(
                post("/inventory/reorder-rules")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{
                            "productId": "p-1",
                            "warehouseId": "wh-1",
                            "reorderPoint": "10"
                        }""",
                    ),
            ).andExpect(status().isForbidden)
    }
}
