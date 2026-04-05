package com.froilan.synectix.controller

import com.froilan.synectix.annotation.LogLevel
import com.froilan.synectix.annotation.Loggable
import com.froilan.synectix.dto.ApiKeyCreatedResponse
import com.froilan.synectix.dto.ApiKeyResponse
import com.froilan.synectix.dto.CreateApiKeyRequest
import com.froilan.synectix.model.User
import com.froilan.synectix.security.SessionContext
import com.froilan.synectix.service.ApiKeyService
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
) {
    @PostMapping
    @PreAuthorize("hasAuthority('apikey:manage')")
    fun createApiKey(
        @Valid @RequestBody request: CreateApiKeyRequest,
    ): ResponseEntity<Any> {
        val (user, sessionContext) = extractUserAndContext() ?: return unauthorized()

        return try {
            val (apiKey, rawKey) =
                apiKeyService.createApiKey(
                    name = request.name,
                    permissions = request.permissions,
                    organizationId = sessionContext.organizationId,
                    createdBy = user.uuid,
                    expiresAt = request.expiresAt,
                )
            ResponseEntity.status(HttpStatus.CREATED).body(
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
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Failed to create API key")))
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('apikey:manage')")
    fun listApiKeys(): ResponseEntity<Any> {
        val (_, sessionContext) = extractUserAndContext() ?: return unauthorized()

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
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val (_, sessionContext) = extractUserAndContext() ?: return unauthorized()

        return try {
            apiKeyService.revokeApiKey(id, sessionContext.organizationId)
            ResponseEntity.ok(mapOf("message" to "API key revoked"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Failed to revoke API key")))
        }
    }

    private fun extractUserAndContext(): Pair<User, SessionContext>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val user = authentication?.principal as? User ?: return null
        val sessionContext = authentication.details as? SessionContext ?: return null
        return user to sessionContext
    }

    private fun unauthorized(): ResponseEntity<Any> =
        ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(mapOf("error" to "Authentication required"))
}
