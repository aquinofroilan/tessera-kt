package com.froilan.synectix.service

import com.froilan.synectix.model.ApiKey
import com.froilan.synectix.repository.ApiKeyRepository
import com.froilan.synectix.security.Permissions
import com.froilan.synectix.util.TokenHasher
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ApiKeyService(
    private val apiKeyRepository: ApiKeyRepository,
    private val mongoTemplate: MongoTemplate,
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
            throw IllegalArgumentException("Invalid permissions: ${invalidPermissions.joinToString()}")
        }
        if (permissions.isEmpty()) {
            throw IllegalArgumentException("At least one permission is required")
        }
        val escalatedPermissions = permissions.filter { it !in creatorPermissions }
        if (escalatedPermissions.isNotEmpty()) {
            throw IllegalArgumentException(
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
                IllegalArgumentException("API key not found")
            }
        if (apiKey.organizationId != organizationId) {
            throw IllegalArgumentException("API key not found")
        }
        if (!apiKey.isActive) {
            throw IllegalArgumentException("API key is already revoked")
        }
        apiKeyRepository.save(apiKey.copy(isActive = false))
    }

    fun authenticateByApiKey(rawKey: String): ApiKey? {
        val keyHash = tokenHasher.hash(rawKey)
        val apiKey = apiKeyRepository.findByKeyHash(keyHash).orElse(null) ?: return null

        if (!apiKey.isActive) return null
        if (apiKey.expiresAt != null && !apiKey.expiresAt.isAfter(LocalDateTime.now())) return null

        mongoTemplate.updateFirst(
            Query.query(Criteria.where("_id").`is`(apiKey.id)),
            Update.update("lastUsedAt", LocalDateTime.now()),
            ApiKey::class.java,
        )

        return apiKey
    }
}
