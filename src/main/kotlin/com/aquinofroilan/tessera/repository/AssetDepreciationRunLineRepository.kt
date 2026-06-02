package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.AssetDepreciationRunLine
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AssetDepreciationRunLineRepository : JpaRepository<AssetDepreciationRunLine, String> {
    fun findByRunId(runId: String): List<AssetDepreciationRunLine>

    fun deleteByRunId(runId: String)
}
