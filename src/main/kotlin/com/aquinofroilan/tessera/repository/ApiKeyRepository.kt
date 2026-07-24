package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.ApiKey
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface ApiKeyRepository : JpaRepository<ApiKey, java.util.UUID> {
    fun findByKeyHash(keyHash: String): Optional<ApiKey>

    fun findByOrganizationIdAndIsActive(
        organizationId: java.util.UUID,
        isActive: Boolean,
    ): List<ApiKey>
}
