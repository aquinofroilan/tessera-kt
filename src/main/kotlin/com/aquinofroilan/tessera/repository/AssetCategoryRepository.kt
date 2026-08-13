package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.AssetCategory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface AssetCategoryRepository : JpaRepository<AssetCategory, UUID> {
    fun findByOrganizationId(organizationId: UUID): List<AssetCategory>

    fun findByOrganizationIdAndIsActiveTrue(organizationId: UUID): List<AssetCategory>

    fun findByOrganizationIdAndCode(
        organizationId: UUID,
        code: String,
    ): Optional<AssetCategory>
}
