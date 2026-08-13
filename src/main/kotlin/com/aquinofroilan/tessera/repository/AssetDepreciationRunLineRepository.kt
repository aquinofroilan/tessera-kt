package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.AssetDepreciationRunLine
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AssetDepreciationRunLineRepository : JpaRepository<AssetDepreciationRunLine, UUID> {
    fun findByRunId(runId: UUID): List<AssetDepreciationRunLine>

    fun deleteByRunId(runId: UUID)
}
