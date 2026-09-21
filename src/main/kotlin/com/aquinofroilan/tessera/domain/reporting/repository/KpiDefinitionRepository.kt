package com.aquinofroilan.tessera.domain.reporting.repository

import com.aquinofroilan.tessera.domain.reporting.model.KpiDefinition
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface KpiDefinitionRepository : JpaRepository<KpiDefinition, UUID> {
    fun findByOrganizationId(organizationId: UUID): List<KpiDefinition>

    fun findByIdAndOrganizationId(
        id: UUID,
        organizationId: UUID,
    ): KpiDefinition?
}
