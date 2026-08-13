package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.AssetStatus
import com.aquinofroilan.tessera.model.FixedAsset
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
