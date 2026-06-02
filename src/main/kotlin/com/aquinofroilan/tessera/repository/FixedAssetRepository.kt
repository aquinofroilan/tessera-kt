package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.AssetStatus
import com.aquinofroilan.tessera.model.FixedAsset
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface FixedAssetRepository : JpaRepository<FixedAsset, String> {
    fun findByOrganizationId(organizationId: String): List<FixedAsset>

    fun findByOrganizationIdAndStatus(
        organizationId: String,
        status: AssetStatus,
    ): List<FixedAsset>

    fun findByOrganizationIdAndCategoryId(
        organizationId: String,
        categoryId: String,
    ): List<FixedAsset>

    fun countByOrganizationId(organizationId: String): Long
}
