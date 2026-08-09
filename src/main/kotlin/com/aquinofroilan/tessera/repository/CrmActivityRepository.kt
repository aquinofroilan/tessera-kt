package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.CrmActivity
import com.aquinofroilan.tessera.model.CrmActivityType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CrmActivityRepository : JpaRepository<CrmActivity, java.util.UUID> {
    fun findByOrganizationIdOrderByOccurredAtDesc(organizationId: java.util.UUID): List<CrmActivity>

    fun findByOrganizationIdAndTypeOrderByOccurredAtDesc(
        organizationId: java.util.UUID,
        type: CrmActivityType,
    ): List<CrmActivity>

    fun findByOrganizationIdAndRelatedLeadIdOrderByOccurredAtDesc(
        organizationId: java.util.UUID,
        relatedLeadId: java.util.UUID,
    ): List<CrmActivity>

    fun findByOrganizationIdAndRelatedOpportunityIdOrderByOccurredAtDesc(
        organizationId: java.util.UUID,
        relatedOpportunityId: java.util.UUID,
    ): List<CrmActivity>

    fun findByOrganizationIdAndRelatedContactIdOrderByOccurredAtDesc(
        organizationId: java.util.UUID,
        relatedContactId: java.util.UUID,
    ): List<CrmActivity>

    fun findByOrganizationIdAndRelatedCustomerIdOrderByOccurredAtDesc(
        organizationId: java.util.UUID,
        relatedCustomerId: java.util.UUID,
    ): List<CrmActivity>

    fun findByOrganizationIdAndOwnerUserIdAndCompletedFalseOrderByDueAtAsc(
        organizationId: java.util.UUID,
        ownerUserId: java.util.UUID,
    ): List<CrmActivity>
}
