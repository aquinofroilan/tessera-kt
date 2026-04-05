package com.froilan.synectix.repository

import com.froilan.synectix.model.ApiKey
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface ApiKeyRepository : MongoRepository<ApiKey, String> {
    fun findByKeyHash(keyHash: String): Optional<ApiKey>

    fun findByOrganizationIdAndIsActive(
        organizationId: String,
        isActive: Boolean,
    ): List<ApiKey>
}
