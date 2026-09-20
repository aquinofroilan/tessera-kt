package com.aquinofroilan.tessera.domain.auth.controller

import com.aquinofroilan.tessera.config.TestSecurityConfig
import com.aquinofroilan.tessera.domain.auth.dto.AuthorizeOAuth2Request
import com.aquinofroilan.tessera.domain.auth.dto.AuthorizeOAuth2Response
import com.aquinofroilan.tessera.domain.auth.dto.RegisterOAuth2ClientRequest
import com.aquinofroilan.tessera.domain.auth.dto.RegisterOAuth2ClientResponse
import com.aquinofroilan.tessera.domain.auth.dto.TokenExchangeRequest
import com.aquinofroilan.tessera.domain.auth.dto.TokenExchangeResponse
import com.aquinofroilan.tessera.domain.auth.service.OAuth2Service
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.security.TesseraPermissionEvaluator
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@WebMvcTest(controllers = [OAuth2Controller::class])
@Import(com.aquinofroilan.tessera.config.WebMvcConfig::class, com.aquinofroilan.tessera.aspect.LoggingAspect::class, TestSecurityConfig::class, TesseraPermissionEvaluator::class)
@ActiveProfiles("test")
class OAuth2ControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    private val objectMapper = ObjectMapper()

    @MockitoBean
    private lateinit var oauth2Service: OAuth2Service

    @MockitoBean
    private lateinit var authContext: AuthenticationContext

    @MockitoBean
    private lateinit var sessionTokenRepository: com.aquinofroilan.tessera.domain.auth.repository.SessionTokenRepository

    @MockitoBean
    private lateinit var userRepository: com.aquinofroilan.tessera.domain.auth.repository.UserRepository

    @MockitoBean
    private lateinit var apiKeyService: com.aquinofroilan.tessera.domain.auth.service.ApiKeyService

    @MockitoBean
    private lateinit var rolePermissionCache: com.aquinofroilan.tessera.security.RolePermissionCache

    private val orgId = UUID.randomUUID()
    private val userId = UUID.randomUUID()

    @Test
    @WithMockUser(authorities = ["ROLE_ADMIN", "settings:write"])
    fun `registerClient should return 200`() {
        whenever(authContext.organizationId()).thenReturn(orgId)

        val request =
            RegisterOAuth2ClientRequest(
                name = "Test Client",
                redirectUris = listOf("https://app.test.com/callback"),
                allowedScopes = listOf("journal:read", "journal:write"),
            )

        val response =
            RegisterOAuth2ClientResponse(
                clientId = "client_123",
                clientSecret = "secret_abc",
                name = "Test Client",
            )

        whenever(oauth2Service.registerClient(eq(orgId), any())).thenReturn(response)

        mockMvc
            .perform(
                post("/api/v1/oauth2/clients")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.clientId").value("client_123"))
            .andExpect(jsonPath("$.clientSecret").value("secret_abc"))
    }

    @Test
    @WithMockUser
    fun `authorize should return authorization code`() {
        whenever(authContext.organizationId()).thenReturn(orgId)
        whenever(authContext.userId()).thenReturn(userId)

        val request =
            AuthorizeOAuth2Request(
                clientId = "client_123",
                redirectUri = "https://app.test.com/callback",
                scopes = listOf("journal:read"),
            )

        val response =
            AuthorizeOAuth2Response(
                code = "auth_code_xyz",
                redirectUri = "https://app.test.com/callback",
            )

        whenever(oauth2Service.generateAuthorizationCode(eq(orgId), eq(userId), any())).thenReturn(response)

        mockMvc
            .perform(
                post("/api/v1/oauth2/authorize")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("auth_code_xyz"))
    }

    @Test
    fun `exchangeToken should return access token and permit all`() {
        // No @WithMockUser because this endpoint should be publicly accessible for token exchange!
        val request =
            TokenExchangeRequest(
                grantType = "authorization_code",
                code = "auth_code_xyz",
                redirectUri = "https://app.test.com/callback",
                clientId = "client_123",
                clientSecret = "secret_abc",
            )

        val response =
            TokenExchangeResponse(
                access_token = "ts_api_testtoken",
                expires_in = 3600,
            )

        whenever(oauth2Service.exchangeToken(any())).thenReturn(response)

        mockMvc
            .perform(
                post("/api/v1/oauth2/token")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.access_token").value("ts_api_testtoken"))
    }
}
