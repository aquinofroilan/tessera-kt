package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.ApiKey
import com.aquinofroilan.tessera.repository.ApiKeyRepository
import com.aquinofroilan.tessera.security.Permissions
import com.aquinofroilan.tessera.util.TokenHasher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class ApiKeyService(
    private val apiKeyRepository: ApiKeyRepository,
    private val tokenHasher: TokenHasher,
) {
    @Transactional
    fun createApiKey(
        name: String,
        permissions: List<String>,
        organizationId: String,
        createdBy: String,
        creatorPermissions: Set<String>,
        expiresAt: LocalDateTime? = null,
    ): Pair<ApiKey, String> {
        val invalidPermissions = permissions.filter { it !in Permissions.ALL_PERMISSIONS }
        if (invalidPermissions.isNotEmpty()) {
            throw BusinessRuleException("Invalid permissions: ${invalidPermissions.joinToString()}")
        }
        if (permissions.isEmpty()) {
            throw BusinessRuleException("At least one permission is required")
        }
        val escalatedPermissions = permissions.filter { it !in creatorPermissions }
        if (escalatedPermissions.isNotEmpty()) {
            throw BusinessRuleException(
                "Cannot grant permissions you do not have: ${escalatedPermissions.joinToString()}",
            )
        }

        val rawKey = tokenHasher.generate(32)
        val apiKey =
            ApiKey(
                name = name,
                keyHash = tokenHasher.hash(rawKey),
                keyPrefix = rawKey.take(8),
                organizationId = organizationId,
                permissions = permissions.distinct(),
                createdBy = createdBy,
                expiresAt = expiresAt,
            )
        val saved = apiKeyRepository.save(apiKey)
        return saved to rawKey
    }

    fun listApiKeys(organizationId: String): List<ApiKey> = apiKeyRepository.findByOrganizationIdAndIsActive(organizationId, true)

    @Transactional
    fun revokeApiKey(
        keyId: String,
        organizationId: String,
    ) {
        val apiKey =
            apiKeyRepository.findById(keyId).orElseThrow {
                ResourceNotFoundException("API key not found")
            }
        if (apiKey.organizationId != organizationId) {
            throw ResourceNotFoundException("API key not found")
        }
        if (!apiKey.isActive) {
            throw BusinessRuleException("API key is already revoked")
        }
        apiKeyRepository.save(apiKey.copy(isActive = false))
    }

    @Transactional
    fun authenticateByApiKey(rawKey: String): ApiKey? {
        val keyHash = tokenHasher.hash(rawKey)
        val apiKey = apiKeyRepository.findByKeyHash(keyHash).orElse(null) ?: return null

        if (!apiKey.isActive) return null
        val expiresAt = apiKey.expiresAt
        if (expiresAt != null && !expiresAt.isAfter(LocalDateTime.now(ZoneOffset.UTC))) return null

        apiKeyRepository.save(apiKey.copy(lastUsedAt = LocalDateTime.now(ZoneOffset.UTC)))

        return apiKey
    }
}
