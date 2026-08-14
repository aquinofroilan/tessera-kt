package com.aquinofroilan.tessera.domain.assets.repository

import com.aquinofroilan.tessera.domain.assets.model.AssetDepreciationRunLine
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AssetDepreciationRunLineRepository : JpaRepository<AssetDepreciationRunLine, UUID> {
    fun findByRunId(runId: UUID): List<AssetDepreciationRunLine>

    fun deleteByRunId(runId: UUID)
}
