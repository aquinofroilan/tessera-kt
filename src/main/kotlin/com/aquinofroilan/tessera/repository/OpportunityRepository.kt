package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.Opportunity
import com.aquinofroilan.tessera.model.OpportunityStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OpportunityRepository : JpaRepository<Opportunity, UUID> {
    fun findByOrganizationId(organizationId: UUID): List<Opportunity>

    fun findByOrganizationIdAndStatus(
        organizationId: UUID,
        status: OpportunityStatus,
    ): List<Opportunity>

    fun findByOrganizationIdAndCustomerId(
        organizationId: UUID,
        customerId: UUID,
    ): List<Opportunity>

    fun findByOrganizationIdAndStageId(
        organizationId: UUID,
        stageId: UUID,
    ): List<Opportunity>

    fun findByOrganizationIdAndOwnerUserId(
        organizationId: UUID,
        ownerUserId: UUID,
    ): List<Opportunity>
}
