package com.froilan.synectix.controller

import com.froilan.synectix.aspect.LoggingAspect
import com.froilan.synectix.config.TestSecurityConfig
import com.froilan.synectix.model.Account
import com.froilan.synectix.model.AccountType
import com.froilan.synectix.model.RoleAssignment
import com.froilan.synectix.model.User
import com.froilan.synectix.repository.InvitationRepository
import com.froilan.synectix.repository.OrganizationRepository
import com.froilan.synectix.repository.PasswordResetTokenRepository
import com.froilan.synectix.repository.RefreshTokenRepository
import com.froilan.synectix.repository.SessionTokenRepository
import com.froilan.synectix.repository.UserRepository
import com.froilan.synectix.security.RolePermissionCache
import com.froilan.synectix.security.SessionContext
import com.froilan.synectix.security.SynectixPermissionEvaluator
import com.froilan.synectix.service.AccountService
import com.froilan.synectix.service.ApiKeyService
import com.froilan.synectix.service.AuthService
import com.froilan.synectix.service.JournalEntryService
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [AccountController::class])
@Import(LoggingAspect::class, TestSecurityConfig::class, SynectixPermissionEvaluator::class)
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
        setupAuthWithPermissions("account:create", "account:read", "account:update", "account:delete")
    }

    private fun setupAuthWithPermissions(vararg permissions: String) {
        val roleAuthorities = testUser.roleAssignments.map { SimpleGrantedAuthority("ROLE_${it.role}") }
        val permissionAuthorities = permissions.map { SimpleGrantedAuthority(it) }
        val authentication = UsernamePasswordAuthenticationToken(testUser, null, roleAuthorities + permissionAuthorities)
        authentication.details = SessionContext(sessionId = "session-123", organizationId = "org-123")
        SecurityContextHolder.getContext().authentication = authentication
    }

    private fun createMockAccount() =
        Account(
            id = "acc-123",
            code = "1000",
            name = "Cash",
            type = AccountType.ASSET,
            organizationId = "org-123",
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
            .andExpect(jsonPath("$.id").value("acc-123"))
            .andExpect(jsonPath("$.code").value("1000"))
            .andExpect(jsonPath("$.name").value("Cash"))
            .andExpect(jsonPath("$.type").value("ASSET"))
            .andExpect(jsonPath("$.organizationId").value("org-123"))
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
            .thenThrow(IllegalArgumentException("Account with code '1000' already exists"))

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
            .andExpect(jsonPath("$[0].id").value("acc-123"))
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
            .andExpect(jsonPath("$.id").value("acc-123"))
            .andExpect(jsonPath("$.code").value("1000"))
            .andExpect(jsonPath("$.name").value("Cash"))
            .andExpect(jsonPath("$.type").value("ASSET"))
    }

    @Test
    fun `GET accounts by id should return 404 when not found`() {
        `when`(accountService.getAccount(any(), any()))
            .thenThrow(IllegalArgumentException("Account not found"))

        mockMvc
            .perform(get("/finance/accounts/nonexistent"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error").value("Account not found"))
    }

    @Test
    fun `PUT accounts should return 200 when updated`() {
        val account = createMockAccount().copy(name = "Updated Cash")
        `when`(accountService.updateAccount(any(), any(), any())).thenReturn(account)

        mockMvc
            .perform(
                put("/finance/accounts/acc-123")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name": "Updated Cash"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value("acc-123"))
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
            .thenThrow(IllegalArgumentException("System accounts cannot be deleted"))

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
