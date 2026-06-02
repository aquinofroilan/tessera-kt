package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.AssetDisposal
import com.aquinofroilan.tessera.model.DisposalStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AssetDisposalRepository : JpaRepository<AssetDisposal, String> {
    fun findByOrganizationIdOrderByDisposalDateDesc(organizationId: String): List<AssetDisposal>

    fun findByOrganizationIdAndAssetId(
        organizationId: String,
        assetId: String,
    ): List<AssetDisposal>

    fun existsByAssetIdAndStatus(
        assetId: String,
        status: DisposalStatus,
    ): Boolean
}
