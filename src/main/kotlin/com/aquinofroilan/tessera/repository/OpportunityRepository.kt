package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.Opportunity
import com.aquinofroilan.tessera.model.OpportunityStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OpportunityRepository : JpaRepository<Opportunity, String> {
    fun findByOrganizationId(organizationId: String): List<Opportunity>

    fun findByOrganizationIdAndStatus(
        organizationId: String,
        status: OpportunityStatus,
    ): List<Opportunity>

    fun findByOrganizationIdAndCustomerId(
        organizationId: String,
        customerId: String,
    ): List<Opportunity>

    fun findByOrganizationIdAndStageId(
        organizationId: String,
        stageId: String,
    ): List<Opportunity>

    fun findByOrganizationIdAndOwnerUserId(
        organizationId: String,
        ownerUserId: String,
    ): List<Opportunity>
}
