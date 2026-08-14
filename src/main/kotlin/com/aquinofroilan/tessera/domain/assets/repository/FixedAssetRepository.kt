package com.aquinofroilan.tessera.domain.assets.repository

import com.aquinofroilan.tessera.domain.assets.model.AssetStatus
import com.aquinofroilan.tessera.domain.assets.model.FixedAsset
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface FixedAssetRepository : JpaRepository<FixedAsset, UUID> {
    fun findByOrganizationId(organizationId: UUID): List<FixedAsset>

    fun findByOrganizationIdAndStatus(
        organizationId: UUID,
        status: AssetStatus,
    ): List<FixedAsset>

    fun findByOrganizationIdAndCategoryId(
        organizationId: UUID,
        categoryId: UUID,
    ): List<FixedAsset>

    fun countByOrganizationId(organizationId: UUID): Long
}
