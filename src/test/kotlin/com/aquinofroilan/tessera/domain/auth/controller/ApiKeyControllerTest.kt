package com.aquinofroilan.tessera.domain.auth.controller

import com.aquinofroilan.tessera.aspect.LoggingAspect
import com.aquinofroilan.tessera.config.TestSecurityConfig
import com.aquinofroilan.tessera.config.WebMvcConfig
import com.aquinofroilan.tessera.domain.auth.model.ApiKey
import com.aquinofroilan.tessera.domain.auth.model.RoleAssignment
import com.aquinofroilan.tessera.domain.auth.model.User
import com.aquinofroilan.tessera.domain.auth.repository.ApiKeyRepository
import com.aquinofroilan.tessera.domain.auth.repository.PasswordResetTokenRepository
import com.aquinofroilan.tessera.domain.auth.repository.RefreshTokenRepository
import com.aquinofroilan.tessera.domain.auth.repository.SessionTokenRepository
import com.aquinofroilan.tessera.domain.auth.repository.UserRepository
import com.aquinofroilan.tessera.domain.auth.service.ApiKeyService
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
import java.util.UUID

@WebMvcTest(controllers = [ApiKeyController::class])
@Import(WebMvcConfig::class, LoggingAspect::class, TestSecurityConfig::class, TesseraPermissionEvaluator::class)
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
        setupAuthWithPermissions("apikey:manage")
    }

    private fun setupAuthWithPermissions(vararg permissions: String) {
        val roleAuthorities = testUser.roleAssignments.map { SimpleGrantedAuthority("ROLE_${it.role}") }
        val permissionAuthorities = permissions.map { SimpleGrantedAuthority(it) }
        val authentication = UsernamePasswordAuthenticationToken(testUser, null, roleAuthorities + permissionAuthorities)
        authentication.details =
            SessionContext(
                sessionId = java.util.UUID.fromString("79c5ca4c-8e48-a8f8-6ffc-5b3271a250aa"),
                organizationId = java.util.UUID.fromString("4abe9f6d-6df3-6e5c-953e-3695db9a5216"),
            )
        SecurityContextHolder.getContext().authentication = authentication
    }

    @Test
    fun `POST api-keys should return 201 with raw key`() {
        val apiKey =
            ApiKey(
                id = java.util.UUID.fromString("6d6d8207-0d68-6a75-5790-f005e3854538"),
                name = "Test Key",
                keyHash = "hashed",
                keyPrefix = "abc12345",
                organizationId = java.util.UUID.fromString("4abe9f6d-6df3-6e5c-953e-3695db9a5216"),
                permissions = listOf("session:read"),
                createdBy = java.util.UUID.fromString("bc17c97c-3d89-7d43-b7e0-7ca0266eafa8"),
            )
        `when`(apiKeyService.createApiKey(any(), any(), any(), any(), any(), anyOrNull()))
            .thenReturn(apiKey to "raw-key-value")

        mockMvc
            .perform(
                post("/api/v1/auth/api-keys")
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
                post("/api/v1/auth/api-keys")
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
                post("/api/v1/auth/api-keys")
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
                    id = java.util.UUID.fromString("21af6b8b-5e22-4483-5679-bbdbe0ab03a6"),
                    name = "Key One",
                    keyHash = "h1",
                    keyPrefix = "prefix01",
                    organizationId = java.util.UUID.fromString("4abe9f6d-6df3-6e5c-953e-3695db9a5216"),
                    permissions = listOf("session:read"),
                    createdBy = java.util.UUID.fromString("bc17c97c-3d89-7d43-b7e0-7ca0266eafa8"),
                ),
            )
        `when`(apiKeyService.listApiKeys(java.util.UUID.fromString("4abe9f6d-6df3-6e5c-953e-3695db9a5216"))).thenReturn(keys)

        mockMvc
            .perform(get("/api/v1/auth/api-keys"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("Key One"))
            .andExpect(jsonPath("$[0].keyPrefix").value("prefix01"))
    }

    @Test
    fun `DELETE api-keys should return 200 when revoked`() {
        mockMvc
            .perform(delete("/api/v1/auth/api-keys/32a14436-99e0-5e9d-9396-3a670fc505c0"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("API key revoked"))
    }

    @Test
    fun `DELETE api-keys should return 400 when already revoked`() {
        `when`(apiKeyService.revokeApiKey(any(), any()))
            .thenThrow(IllegalArgumentException("API key is already revoked"))

        mockMvc
            .perform(delete("/api/v1/auth/api-keys/32a14436-99e0-5e9d-9396-3a670fc505c0"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("API key is already revoked"))
    }

    @Test
    fun `POST api-keys should return 403 without apikey manage permission`() {
        setupAuthWithPermissions()

        mockMvc
            .perform(
                post("/api/v1/auth/api-keys")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name": "Key", "permissions": ["session:read"]}"""),
            ).andExpect(status().isForbidden)
    }
}
