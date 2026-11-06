package com.aquinofroilan.tessera.domain.auth.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.auth.dto.AuthorizeOAuth2Request
import com.aquinofroilan.tessera.domain.auth.dto.AuthorizeOAuth2Response
import com.aquinofroilan.tessera.domain.auth.dto.RegisterOAuth2ClientRequest
import com.aquinofroilan.tessera.domain.auth.dto.RegisterOAuth2ClientResponse
import com.aquinofroilan.tessera.domain.auth.dto.TokenExchangeRequest
import com.aquinofroilan.tessera.domain.auth.dto.TokenExchangeResponse
import com.aquinofroilan.tessera.domain.auth.service.OAuth2Service
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.security.CurrentOrganizationId
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/oauth2")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class OAuth2Controller(
    private val oauth2Service: OAuth2Service,
    private val authContext: AuthenticationContext,
) {
    @PostMapping("/clients")
    @PreAuthorize("hasAuthority('settings:write')")
    fun registerClient(
        @CurrentOrganizationId orgId: UUID,
        @RequestBody request: RegisterOAuth2ClientRequest,
    ): ResponseEntity<RegisterOAuth2ClientResponse> = ResponseEntity.ok(oauth2Service.registerClient(orgId, request))

    @PostMapping("/authorize")
    fun authorize(
        @CurrentOrganizationId orgId: UUID,
        @RequestBody request: AuthorizeOAuth2Request,
    ): ResponseEntity<AuthorizeOAuth2Response> {
        // Any authenticated user can authorize a client
        val userId = authContext.userId()!!
        return ResponseEntity.ok(oauth2Service.generateAuthorizationCode(orgId, userId, request))
    }

    // This endpoint must be publicly accessible (PermitAll in SecurityConfig)
    @PostMapping("/token", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    fun exchangeToken(
        @RequestParam("grant_type") grantType: String,
        @RequestParam("code") code: String,
        @RequestParam("redirect_uri") redirectUri: String,
        @RequestParam("client_id") clientId: String,
        @RequestParam("client_secret") clientSecret: String,
    ): ResponseEntity<TokenExchangeResponse> =
        ResponseEntity.ok(
            oauth2Service.exchangeToken(
                TokenExchangeRequest(
                    grantType = grantType,
                    code = code,
                    redirectUri = redirectUri,
                    clientId = clientId,
                    clientSecret = clientSecret,
                ),
            ),
        )
}
