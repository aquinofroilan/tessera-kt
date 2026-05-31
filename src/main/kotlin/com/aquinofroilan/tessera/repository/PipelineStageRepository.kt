package com.aquinofroilan.tessera.repository
import java.util.UUID

import com.aquinofroilan.tessera.model.PipelineStage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface PipelineStageRepository : JpaRepository<PipelineStage, UUID> {
    fun findByOrganizationIdOrderBySortOrderAsc(organizationId: UUID): List<PipelineStage>

    fun findByOrganizationIdAndIsActiveOrderBySortOrderAsc(
        organizationId: UUID,
        isActive: Boolean,
    ): List<PipelineStage>

    fun findByOrganizationIdAndCode(
        organizationId: UUID,
        code: String,
    ): Optional<PipelineStage>
}
