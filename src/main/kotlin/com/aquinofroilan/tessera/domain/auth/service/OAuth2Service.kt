package com.aquinofroilan.tessera.domain.auth.service

import com.aquinofroilan.tessera.domain.auth.dto.AuthorizeOAuth2Request
import com.aquinofroilan.tessera.domain.auth.dto.AuthorizeOAuth2Response
import com.aquinofroilan.tessera.domain.auth.dto.RegisterOAuth2ClientRequest
import com.aquinofroilan.tessera.domain.auth.dto.RegisterOAuth2ClientResponse
import com.aquinofroilan.tessera.domain.auth.dto.TokenExchangeRequest
import com.aquinofroilan.tessera.domain.auth.dto.TokenExchangeResponse
import com.aquinofroilan.tessera.domain.auth.model.OAuth2Client
import com.aquinofroilan.tessera.domain.auth.model.OAuth2Code
import com.aquinofroilan.tessera.domain.auth.repository.OAuth2ClientRepository
import com.aquinofroilan.tessera.domain.auth.repository.OAuth2CodeRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.security.Permissions
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Base64
import java.util.UUID

@Service
class OAuth2Service(
    private val clientRepository: OAuth2ClientRepository,
    private val codeRepository: OAuth2CodeRepository,
    private val apiKeyService: ApiKeyService,
    private val passwordEncoder: PasswordEncoder,
) {
    private val secureRandom = SecureRandom()

    private fun generateRandomString(length: Int): String {
        val bytes = ByteArray(length)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    @Transactional
    fun registerClient(
        organizationId: UUID,
        request: RegisterOAuth2ClientRequest,
    ): RegisterOAuth2ClientResponse {
        val clientId = "client_${generateRandomString(16)}"
        val clientSecret = generateRandomString(32)

        val client =
            OAuth2Client(
                organizationId = organizationId,
                clientId = clientId,
                clientSecretHash = passwordEncoder.encode(clientSecret)!!,
                name = request.name,
                redirectUris = request.redirectUris,
                allowedScopes = request.allowedScopes,
            )

        clientRepository.save(client)

        return RegisterOAuth2ClientResponse(
            clientId = clientId,
            clientSecret = clientSecret,
            name = request.name,
        )
    }

    @Transactional
    fun generateAuthorizationCode(
        organizationId: UUID,
        userId: UUID,
        request: AuthorizeOAuth2Request,
    ): AuthorizeOAuth2Response {
        val client =
            clientRepository.findByClientId(request.clientId)
                ?: throw ResourceNotFoundException("Invalid client_id")

        if (!client.isActive) {
            throw BusinessRuleException("Client is inactive")
        }

        if (!client.redirectUris.contains(request.redirectUri)) {
            throw BusinessRuleException("Invalid redirect_uri")
        }

        val invalidScopes = request.scopes.filter { !client.allowedScopes.contains(it) }
        if (invalidScopes.isNotEmpty()) {
            throw BusinessRuleException("Invalid scopes requested: $invalidScopes")
        }

        val code = "auth_${generateRandomString(24)}"

        val oauth2Code =
            OAuth2Code(
                code = code,
                clientId = client.clientId,
                organizationId = organizationId,
                userId = userId,
                scopes = request.scopes,
                redirectUri = request.redirectUri,
                expiresAt = LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10),
            )

        codeRepository.save(oauth2Code)

        return AuthorizeOAuth2Response(
            code = code,
            redirectUri = request.redirectUri,
        )
    }

    @Transactional
    fun exchangeToken(request: TokenExchangeRequest): TokenExchangeResponse {
        if (request.grantType != "authorization_code") {
            throw BusinessRuleException("Unsupported grant_type")
        }

        val client =
            clientRepository.findByClientId(request.clientId)
                ?: throw BusinessRuleException("Invalid client credentials")

        if (!passwordEncoder.matches(request.clientSecret, client.clientSecretHash)) {
            throw BusinessRuleException("Invalid client credentials")
        }

        val oauth2Code =
            codeRepository
                .findById(request.code)
                .orElseThrow { BusinessRuleException("Invalid authorization code") }

        if (oauth2Code.clientId != request.clientId) {
            throw BusinessRuleException("Invalid authorization code for this client")
        }

        if (oauth2Code.redirectUri != request.redirectUri) {
            throw BusinessRuleException("Invalid redirect_uri")
        }

        if (oauth2Code.expiresAt.isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
            codeRepository.delete(oauth2Code)
            throw BusinessRuleException("Authorization code expired")
        }

        // Single-use code
        codeRepository.delete(oauth2Code)

        // Generate an API Key internally scoped to the requested permissions
        val (_, rawKey) =
            apiKeyService.createApiKey(
                name = "OAuth2: ${client.name}",
                permissions = oauth2Code.scopes,
                organizationId = oauth2Code.organizationId,
                createdBy = oauth2Code.userId,
                creatorPermissions = Permissions.ALL_PERMISSIONS.toSet(),
                expiresAt = null,
            )

        // Return standard OAuth2 JSON response
        return TokenExchangeResponse(
            access_token = rawKey,
            expires_in = 31536000, // default 1 year for now, could be short-lived
        )
    }
}
