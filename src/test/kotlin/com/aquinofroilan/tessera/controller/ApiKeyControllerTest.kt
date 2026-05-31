package com.aquinofroilan.tessera.controller

import com.aquinofroilan.tessera.aspect.LoggingAspect
import com.aquinofroilan.tessera.config.TestSecurityConfig
import com.aquinofroilan.tessera.model.ApiKey
import com.aquinofroilan.tessera.model.RoleAssignment
import com.aquinofroilan.tessera.model.User
import com.aquinofroilan.tessera.repository.ApiKeyRepository
import com.aquinofroilan.tessera.repository.OrganizationRepository
import com.aquinofroilan.tessera.repository.PasswordResetTokenRepository
import com.aquinofroilan.tessera.repository.RefreshTokenRepository
import com.aquinofroilan.tessera.repository.SessionTokenRepository
import com.aquinofroilan.tessera.repository.UserRepository
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.security.RolePermissionCache
import com.aquinofroilan.tessera.security.SessionContext
import com.aquinofroilan.tessera.security.TesseraPermissionEvaluator
import com.aquinofroilan.tessera.service.ApiKeyService
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [ApiKeyController::class])
@Import(LoggingAspect::class, TestSecurityConfig::class, TesseraPermissionEvaluator::class)
@ActiveProfiles("test")
class ApiKeyControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var apiKeyService: ApiKeyService

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
    private lateinit var apiKeyRepository: ApiKeyRepository

    @MockitoBean
    private lateinit var tokenHasher: TokenHasher

    @MockitoBean
    private lateinit var rolePermissionCache: RolePermissionCache

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
        setupAuthWithPermissions("apikey:manage")
    }

    private fun setupAuthWithPermissions(vararg permissions: String) {
        val roleAuthorities = testUser.roleAssignments.map { SimpleGrantedAuthority("ROLE_${it.role}") }
        val permissionAuthorities = permissions.map { SimpleGrantedAuthority(it) }
        val authentication = UsernamePasswordAuthenticationToken(testUser, null, roleAuthorities + permissionAuthorities)
        authentication.details = SessionContext(sessionId = "session-123", organizationId = "org-123")
        SecurityContextHolder.getContext().authentication = authentication
    }

    @Test
    fun `POST api-keys should return 201 with raw key`() {
        val apiKey =
            ApiKey(
                id = "key-123",
                name = "Test Key",
                keyHash = "hashed",
                keyPrefix = "abc12345",
                organizationId = "org-123",
                permissions = listOf("session:read"),
                createdBy = "user-123",
            )
        `when`(apiKeyService.createApiKey(any(), any(), any(), any(), any(), anyOrNull()))
            .thenReturn(apiKey to "raw-key-value")

        mockMvc
            .perform(
                post("/auth/api-keys")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name": "Test Key", "permissions": ["session:read"]}"""),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.rawKey").value("raw-key-value"))
            .andExpect(jsonPath("$.name").value("Test Key"))
            .andExpect(jsonPath("$.keyPrefix").value("abc12345"))
    }

    @Test
    fun `POST api-keys should return 400 when name is blank`() {
        mockMvc
            .perform(
                post("/auth/api-keys")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name": "", "permissions": ["session:read"]}"""),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `POST api-keys should return 400 with invalid permissions`() {
        `when`(apiKeyService.createApiKey(any(), any(), any(), any(), any(), anyOrNull()))
            .thenThrow(IllegalArgumentException("Invalid permissions: bad:perm"))

        mockMvc
            .perform(
                post("/auth/api-keys")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name": "Bad Key", "permissions": ["bad:perm"]}"""),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Invalid permissions: bad:perm"))
    }

    @Test
    fun `GET api-keys should return 200 with key list`() {
        val keys =
            listOf(
                ApiKey(
                    id = "key-1",
                    name = "Key One",
                    keyHash = "h1",
                    keyPrefix = "prefix01",
                    organizationId = "org-123",
                    permissions = listOf("session:read"),
                    createdBy = "user-123",
                ),
            )
        `when`(apiKeyService.listApiKeys("org-123")).thenReturn(keys)

        mockMvc
            .perform(get("/auth/api-keys"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("Key One"))
            .andExpect(jsonPath("$[0].keyPrefix").value("prefix01"))
    }

    @Test
    fun `DELETE api-keys should return 200 when revoked`() {
        mockMvc
            .perform(delete("/auth/api-keys/key-123"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("API key revoked"))
    }

    @Test
    fun `DELETE api-keys should return 400 when already revoked`() {
        `when`(apiKeyService.revokeApiKey(any(), any()))
            .thenThrow(IllegalArgumentException("API key is already revoked"))

        mockMvc
            .perform(delete("/auth/api-keys/key-123"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("API key is already revoked"))
    }

    @Test
    fun `POST api-keys should return 403 without apikey manage permission`() {
        setupAuthWithPermissions()

        mockMvc
            .perform(
                post("/auth/api-keys")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name": "Key", "permissions": ["session:read"]}"""),
            ).andExpect(status().isForbidden)
    }
}
