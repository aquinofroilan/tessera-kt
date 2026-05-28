package com.loom.synectix.controller

import com.loom.synectix.aspect.LoggingAspect
import com.loom.synectix.config.TestSecurityConfig
import com.loom.synectix.exception.BusinessRuleException
import com.loom.synectix.exception.ResourceNotFoundException
import com.loom.synectix.model.RoleAssignment
import com.loom.synectix.model.User
import com.loom.synectix.model.Warehouse
import com.loom.synectix.repository.InvitationRepository
import com.loom.synectix.repository.OrganizationRepository
import com.loom.synectix.repository.PasswordResetTokenRepository
import com.loom.synectix.repository.RefreshTokenRepository
import com.loom.synectix.repository.SessionTokenRepository
import com.loom.synectix.repository.UserRepository
import com.loom.synectix.security.AuthenticationContext
import com.loom.synectix.security.RolePermissionCache
import com.loom.synectix.security.SessionContext
import com.loom.synectix.security.SynectixPermissionEvaluator
import com.loom.synectix.service.AccountService
import com.loom.synectix.service.ApiKeyService
import com.loom.synectix.service.AuthService
import com.loom.synectix.service.JournalEntryService
import com.loom.synectix.service.WarehouseService
import com.loom.synectix.util.TokenHasher
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@WebMvcTest(controllers = [WarehouseController::class])
@Import(LoggingAspect::class, TestSecurityConfig::class, SynectixPermissionEvaluator::class)
@ActiveProfiles("test")
class WarehouseControllerTest {
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
    private lateinit var warehouseService: WarehouseService

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

    private fun createMockWarehouse() =
        Warehouse(
            id = "wh-123",
            code = "MAIN",
            name = "Main Warehouse",
            description = "Primary",
            addressLine = "123 Main St",
            city = "Springfield",
            country = "US",
            allowNegativeStock = false,
            organizationId = "org-123",
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

    @Test
    fun `POST warehouses should return 201 when created`() {
        `when`(warehouseService.createWarehouse(any(), any())).thenReturn(createMockWarehouse())

        mockMvc
            .perform(
                post("/inventory/warehouses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{
                            "code": "MAIN",
                            "name": "Main Warehouse",
                            "allowNegativeStock": false
                        }""",
                    ),
            ).andExpect(status().isCreated)
    }

    @Test
    fun `POST warehouses should return 400 when code is blank`() {
        mockMvc
            .perform(
                post("/inventory/warehouses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{
                            "code": "",
                            "name": "Main"
                        }""",
                    ),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `POST warehouses should return 400 when duplicate code in org`() {
        `when`(warehouseService.createWarehouse(any(), any()))
            .thenThrow(BusinessRuleException("Warehouse with code 'MAIN' already exists in this organization"))

        mockMvc
            .perform(
                post("/inventory/warehouses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{
                            "code": "MAIN",
                            "name": "Main"
                        }""",
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Warehouse with code 'MAIN' already exists in this organization"))
    }

    @Test
    fun `GET warehouses should return 200 with list`() {
        `when`(warehouseService.listWarehouses(any(), any(), anyOrNull())).thenReturn(listOf(createMockWarehouse()))

        mockMvc
            .perform(get("/inventory/warehouses"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
    }

    @Test
    fun `GET warehouses should support search filter`() {
        `when`(warehouseService.listWarehouses(any(), any(), any())).thenReturn(listOf(createMockWarehouse()))

        mockMvc
            .perform(get("/inventory/warehouses").param("search", "MAIN"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
    }

    @Test
    fun `GET warehouse by id should return 200`() {
        `when`(warehouseService.getWarehouse(any(), any())).thenReturn(createMockWarehouse())

        mockMvc
            .perform(get("/inventory/warehouses/wh-123"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value("wh-123"))
            .andExpect(jsonPath("$.code").value("MAIN"))
    }

    @Test
    fun `GET warehouse by id should return 404 when not found`() {
        `when`(warehouseService.getWarehouse(any(), any()))
            .thenThrow(ResourceNotFoundException("Warehouse not found"))

        mockMvc
            .perform(get("/inventory/warehouses/nonexistent"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `PATCH warehouses should return 200 when updated`() {
        val updated = createMockWarehouse().copy(name = "Renamed")
        `when`(warehouseService.updateWarehouse(any(), any(), any())).thenReturn(updated)

        mockMvc
            .perform(
                patch("/inventory/warehouses/wh-123")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name": "Renamed"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Renamed"))
    }

    @Test
    fun `DELETE warehouses should return 200 when soft deleted`() {
        `when`(warehouseService.deleteWarehouse(any(), any())).thenReturn(createMockWarehouse().copy(isActive = false))

        mockMvc
            .perform(delete("/inventory/warehouses/wh-123"))
            .andExpect(status().isOk)
    }

    @Test
    fun `POST warehouses should return 403 without inventory write`() {
        setupAuthWithPermissions("inventory:read")

        mockMvc
            .perform(
                post("/inventory/warehouses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"code": "MAIN", "name": "Main"}"""),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `GET warehouses should return 403 without inventory read`() {
        setupAuthWithPermissions("inventory:write")

        mockMvc
            .perform(get("/inventory/warehouses"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `PATCH warehouses should return 403 without inventory write`() {
        setupAuthWithPermissions("inventory:read")

        mockMvc
            .perform(
                patch("/inventory/warehouses/wh-123")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name": "Renamed"}"""),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `DELETE warehouses should return 403 without inventory write`() {
        setupAuthWithPermissions("inventory:read")

        mockMvc
            .perform(delete("/inventory/warehouses/wh-123"))
            .andExpect(status().isForbidden)
    }
}
