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
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
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
    @PostMapping("/token")
    fun exchangeToken(
        @RequestBody request: TokenExchangeRequest,
    ): ResponseEntity<TokenExchangeResponse> = ResponseEntity.ok(oauth2Service.exchangeToken(request))
}
