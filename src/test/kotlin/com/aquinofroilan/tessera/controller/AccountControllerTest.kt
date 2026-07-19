package com.aquinofroilan.tessera.controller

import com.aquinofroilan.tessera.aspect.LoggingAspect
import com.aquinofroilan.tessera.config.TestSecurityConfig
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.Account
import com.aquinofroilan.tessera.model.AccountType
import com.aquinofroilan.tessera.model.RoleAssignment
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [AccountController::class])
@Import(LoggingAspect::class, TestSecurityConfig::class, TesseraPermissionEvaluator::class)
@ActiveProfiles("test")
class AccountControllerTest {
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
    private lateinit var authenticationContext: AuthenticationContext

    private val testUser =
        User(
            uuid = java.util.UUID.fromString("bc17c97c-3d89-7d43-b7e0-7ca0266eafa8"),
            username = "testuser",
            email = "test@example.com",
            firstName = "Test",
            lastName = "User",
            passwordHash = "encoded",
            organizationId = java.util.UUID.fromString("4abe9f6d-6df3-6e5c-953e-3695db9a5216"),
            roleAssignments = listOf(RoleAssignment("OWNER", java.util.UUID.fromString("4abe9f6d-6df3-6e5c-953e-3695db9a5216"))),
        )

    @BeforeEach
    fun setup() {
        setupAuthWithPermissions("account:create", "account:read", "account:update", "account:delete")
        `when`(authenticationContext.organizationId()).thenReturn(java.util.UUID.fromString("4abe9f6d-6df3-6e5c-953e-3695db9a5216"))
        `when`(authenticationContext.userId()).thenReturn(java.util.UUID.fromString("bc17c97c-3d89-7d43-b7e0-7ca0266eafa8"))
    }

    private fun setupAuthWithPermissions(vararg permissions: String) {
        val roleAuthorities = testUser.roleAssignments.map { SimpleGrantedAuthority("ROLE_${it.role}") }
        val permissionAuthorities = permissions.map { SimpleGrantedAuthority(it) }
        val authentication = UsernamePasswordAuthenticationToken(testUser, null, roleAuthorities + permissionAuthorities)
        authentication.details = SessionContext(sessionId = java.util.UUID.fromString("79c5ca4c-8e48-a8f8-6ffc-5b3271a250aa"), organizationId = java.util.UUID.fromString("4abe9f6d-6df3-6e5c-953e-3695db9a5216"))
        SecurityContextHolder.getContext().authentication = authentication
    }

    private fun createMockAccount() =
        Account(
            id = java.util.UUID.fromString("12a14436-99e0-5e9d-9396-3a670fc505c0"),
            code = "1000",
            name = "Cash",
            type = AccountType.ASSET,
            organizationId = java.util.UUID.fromString("4abe9f6d-6df3-6e5c-953e-3695db9a5216"),
            isSystemAccount = false,
        )

    @Test
    fun `POST accounts should return 201 when created`() {
        val account = createMockAccount()
        `when`(accountService.createAccount(any(), any())).thenReturn(account)

        mockMvc
            .perform(
                post("/finance/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"code": "1000", "name": "Cash", "type": "ASSET"}"""),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value("12a14436-99e0-5e9d-9396-3a670fc505c0"))
            .andExpect(jsonPath("$.code").value("1000"))
            .andExpect(jsonPath("$.name").value("Cash"))
            .andExpect(jsonPath("$.type").value("ASSET"))
            .andExpect(jsonPath("$.organizationId").value("4abe9f6d-6df3-6e5c-953e-3695db9a5216"))
            .andExpect(jsonPath("$.systemAccount").value(false))
    }

    @Test
    fun `POST accounts should return 400 when code is blank`() {
        mockMvc
            .perform(
                post("/finance/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"code": "", "name": "Cash", "type": "ASSET"}"""),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `POST accounts should return 400 when duplicate code`() {
        `when`(accountService.createAccount(any(), any()))
            .thenThrow(BusinessRuleException("Account with code '1000' already exists"))

        mockMvc
            .perform(
                post("/finance/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"code": "1000", "name": "Cash", "type": "ASSET"}"""),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Account with code '1000' already exists"))
    }

    @Test
    fun `GET accounts should return 200 with account list`() {
        val accounts = listOf(createMockAccount())
        `when`(accountService.listAccounts(any(), anyOrNull(), anyOrNull())).thenReturn(accounts)

        mockMvc
            .perform(get("/finance/accounts"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value("12a14436-99e0-5e9d-9396-3a670fc505c0"))
            .andExpect(jsonPath("$[0].code").value("1000"))
            .andExpect(jsonPath("$[0].name").value("Cash"))
            .andExpect(jsonPath("$[0].type").value("ASSET"))
    }

    @Test
    fun `GET accounts by id should return 200`() {
        val account = createMockAccount()
        `when`(accountService.getAccount(any(), any())).thenReturn(account)

        mockMvc
            .perform(get("/finance/accounts/acc-123"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value("12a14436-99e0-5e9d-9396-3a670fc505c0"))
            .andExpect(jsonPath("$.code").value("1000"))
            .andExpect(jsonPath("$.name").value("Cash"))
            .andExpect(jsonPath("$.type").value("ASSET"))
    }

    @Test
    fun `GET accounts by id should return 404 when not found`() {
        `when`(accountService.getAccount(any(), any()))
            .thenThrow(ResourceNotFoundException("Account not found"))

        mockMvc
            .perform(get("/finance/accounts/nonexistent"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error").value("Account not found"))
    }

    @Test
    fun `PUT accounts should return 200 when updated`() {
        val account = createMockAccount().apply { name = "Updated Cash" }
        `when`(accountService.updateAccount(any(), any(), any())).thenReturn(account)

        mockMvc
            .perform(
                put("/finance/accounts/acc-123")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name": "Updated Cash"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value("12a14436-99e0-5e9d-9396-3a670fc505c0"))
            .andExpect(jsonPath("$.name").value("Updated Cash"))
    }

    @Test
    fun `DELETE accounts should return 200 when deactivated`() {
        mockMvc
            .perform(delete("/finance/accounts/acc-123"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Account deactivated"))
    }

    @Test
    fun `DELETE accounts should return 400 for system account`() {
        `when`(accountService.deleteAccount(any(), any()))
            .thenThrow(BusinessRuleException("System accounts cannot be deleted"))

        mockMvc
            .perform(delete("/finance/accounts/acc-123"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("System accounts cannot be deleted"))
    }

    @Test
    fun `POST accounts should return 403 without account create permission`() {
        setupAuthWithPermissions("account:read")

        mockMvc
            .perform(
                post("/finance/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"code": "1000", "name": "Cash", "type": "ASSET"}"""),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `DELETE accounts should return 403 without account delete permission`() {
        setupAuthWithPermissions("account:read")

        mockMvc
            .perform(delete("/finance/accounts/acc-123"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `GET accounts should return 400 for invalid type query param`() {
        mockMvc
            .perform(get("/finance/accounts").param("type", "INVALID"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Invalid account type 'INVALID'"))
    }
}
