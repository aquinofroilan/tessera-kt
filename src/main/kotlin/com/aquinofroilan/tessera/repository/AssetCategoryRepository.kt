package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.AssetCategory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface AssetCategoryRepository : JpaRepository<AssetCategory, String> {
    fun findByOrganizationId(organizationId: String): List<AssetCategory>

    fun findByOrganizationIdAndIsActiveTrue(organizationId: String): List<AssetCategory>

    fun findByOrganizationIdAndCode(
        organizationId: String,
        code: String,
    ): Optional<AssetCategory>
}
