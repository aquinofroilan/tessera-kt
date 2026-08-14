package com.aquinofroilan.tessera.domain.assets.repository

import com.aquinofroilan.tessera.domain.assets.model.AssetDisposal
import com.aquinofroilan.tessera.domain.assets.model.DisposalStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AssetDisposalRepository : JpaRepository<AssetDisposal, UUID> {
    fun findByOrganizationIdOrderByDisposalDateDesc(organizationId: UUID): List<AssetDisposal>

    fun findByOrganizationIdAndAssetId(
        organizationId: UUID,
        assetId: UUID,
    ): List<AssetDisposal>

    fun existsByAssetIdAndStatus(
        assetId: UUID,
        status: DisposalStatus,
    ): Boolean
}
