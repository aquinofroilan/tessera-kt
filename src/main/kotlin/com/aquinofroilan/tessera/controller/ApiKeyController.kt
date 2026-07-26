package com.aquinofroilan.tessera.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.dto.ApiKeyCreatedResponse
import com.aquinofroilan.tessera.dto.ApiKeyResponse
import com.aquinofroilan.tessera.dto.CreateApiKeyRequest
import com.aquinofroilan.tessera.model.User
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.security.SessionContext
import com.aquinofroilan.tessera.service.ApiKeyService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth/api-keys")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class ApiKeyController(
    private val apiKeyService: ApiKeyService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('apikey:manage')")
    fun createApiKey(
        @Valid @RequestBody request: CreateApiKeyRequest,
    ): ResponseEntity<Any> {
        val (user, sessionContext) = extractUserAndContext() ?: return authContext.unauthorized()
        val authentication = SecurityContextHolder.getContext().authentication ?: return authContext.unauthorized()
        val creatorPermissions = authentication.authorities.mapNotNull { it.authority }.toSet()

        val (apiKey, rawKey) =
            apiKeyService.createApiKey(
                name = request.name,
                permissions = request.permissions,
                organizationId = sessionContext.organizationId,
                createdBy = user.uuid,
                creatorPermissions = creatorPermissions,
                expiresAt = request.expiresAt,
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiKeyCreatedResponse(
                id = apiKey.id,
                name = apiKey.name,
                rawKey = rawKey,
                keyPrefix = apiKey.keyPrefix,
                permissions = apiKey.permissions,
                organizationId = apiKey.organizationId,
                expiresAt = apiKey.expiresAt?.toString(),
                createdAt = apiKey.createdAt?.toString(),
            ),
        )
    }

    @GetMapping
    @PreAuthorize("hasAuthority('apikey:manage')")
    fun listApiKeys(): ResponseEntity<Any> {
        val (_, sessionContext) = extractUserAndContext() ?: return authContext.unauthorized()

        val keys =
            apiKeyService.listApiKeys(sessionContext.organizationId).map { key ->
                ApiKeyResponse(
                    id = key.id,
                    name = key.name,
                    keyPrefix = key.keyPrefix,
                    permissions = key.permissions,
                    organizationId = key.organizationId,
                    isActive = key.isActive,
                    lastUsedAt = key.lastUsedAt?.toString(),
                    expiresAt = key.expiresAt?.toString(),
                    createdAt = key.createdAt?.toString(),
                )
            }
        return ResponseEntity.ok(keys)
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('apikey:manage')")
    fun revokeApiKey(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val (_, sessionContext) = extractUserAndContext() ?: return authContext.unauthorized()

        apiKeyService.revokeApiKey(id, sessionContext.organizationId)
        return ResponseEntity.ok(mapOf("message" to "API key revoked"))
    }

    private fun extractUserAndContext(): Pair<User, SessionContext>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val user = authentication?.principal as? User ?: return null
        val sessionContext = authentication.details as? SessionContext ?: return null
        return user to sessionContext
    }
}
