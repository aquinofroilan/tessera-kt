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
import com.aquinofroilan.tessera.domain.sales.model.Customer
import com.aquinofroilan.tessera.domain.sales.model.CustomerSegment
import com.aquinofroilan.tessera.domain.sales.service.CustomerService
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.security.RolePermissionCache
import com.aquinofroilan.tessera.security.SessionContext
import com.aquinofroilan.tessera.security.TesseraPermissionEvaluator
import com.aquinofroilan.tessera.util.TokenHasher
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.util.UUID

@WebMvcTest(controllers = [CustomerController::class])
@Import(WebMvcConfig::class, LoggingAspect::class, TestSecurityConfig::class, TesseraPermissionEvaluator::class)
@ActiveProfiles("test")
class CustomerControllerTest {
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
    private lateinit var customerService: CustomerService

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
        setupAuthWithPermissions("ar:read", "ar:create")
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

    private fun createMockCustomer() =
        Customer(
            id = UUID.fromString("00000000-0000-0000-0000-000000000199"),
            name = "Test Customer",
            contactName = "John Doe",
            contactEmail = "john@example.com",
            contactPhone = "+1234567890",
            paymentTermDays = 30,
            organizationId = UUID.fromString("00000000-0000-0000-0000-000000000199"),
            customerSegment = CustomerSegment.RETAIL,
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

    @Test
    fun `POST create customer should return 201`() {
        val customer = createMockCustomer()
        `when`(customerService.createCustomer(any(), any())).thenReturn(customer)

        mockMvc
            .perform(
                post("/api/v1/sales/customers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{
                            "name": "Test Customer",
                            "contactName": "John Doe",
                            "contactEmail": "john@example.com",
                            "contactPhone": "+1234567890",
                            "paymentTermDays": 30,
                            "customerSegment": "RETAIL"
                        }""",
                    ),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("Test Customer"))
    }

    @Test
    fun `GET list customers should return 200`() {
        val customers = listOf(createMockCustomer())
        `when`(customerService.listCustomers(any())).thenReturn(customers)

        mockMvc
            .perform(get("/api/v1/sales/customers"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("Test Customer"))
    }

    @Test
    fun `GET customer by id should return 200`() {
        val customer = createMockCustomer()
        `when`(customerService.getCustomer(any(), any())).thenReturn(customer)

        mockMvc
            .perform(get("/api/v1/sales/customers/00000000-0000-0000-0000-000000000199"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value("00000000-0000-0000-0000-000000000199"))
            .andExpect(jsonPath("$.name").value("Test Customer"))
    }

    @Test
    fun `PUT update customer should return 200`() {
        val updated = createMockCustomer().apply { name = "Updated Customer" }
        `when`(customerService.updateCustomer(any(), any(), any())).thenReturn(updated)

        mockMvc
            .perform(
                put("/api/v1/sales/customers/00000000-0000-0000-0000-000000000199")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name": "Updated Customer"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Updated Customer"))
    }

    @Test
    fun `DELETE customer should return 200`() {
        val deleted = createMockCustomer().apply { isActive = false }
        `when`(customerService.deleteCustomer(any(), any())).thenReturn(deleted)

        mockMvc
            .perform(delete("/api/v1/sales/customers/00000000-0000-0000-0000-000000000199"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.active").value(false))
    }

    @Test
    fun `GET list customers should return 401 missing auth`() {
        SecurityContextHolder.clearContext()
        `when`(authenticationContext.organizationId()).thenReturn(null)
        `when`(authenticationContext.userId()).thenReturn(null)

        mockMvc
            .perform(get("/api/v1/sales/customers"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST create customer should return 403 missing permission`() {
        setupAuthWithPermissions("ar:read")

        mockMvc
            .perform(
                post("/api/v1/sales/customers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{
                            "name": "Test Customer",
                            "paymentTermDays": 30,
                            "customerSegment": "RETAIL"
                        }""",
                    ),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `PUT update customer should return 403 missing permission`() {
        setupAuthWithPermissions("ar:read")

        mockMvc
            .perform(
                put("/api/v1/sales/customers/00000000-0000-0000-0000-000000000199")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name": "Updated Customer"}"""),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `DELETE customer should return 403 missing permission`() {
        setupAuthWithPermissions("ar:read")

        mockMvc
            .perform(delete("/api/v1/sales/customers/00000000-0000-0000-0000-000000000199"))
            .andExpect(status().isForbidden)
    }
}
