package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.CrmActivity
import com.aquinofroilan.tessera.model.CrmActivityType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CrmActivityRepository : JpaRepository<CrmActivity, String> {
    fun findByOrganizationIdOrderByOccurredAtDesc(organizationId: String): List<CrmActivity>

    fun findByOrganizationIdAndTypeOrderByOccurredAtDesc(
        organizationId: String,
        type: CrmActivityType,
    ): List<CrmActivity>

    fun findByOrganizationIdAndRelatedLeadIdOrderByOccurredAtDesc(
        organizationId: String,
        relatedLeadId: String,
    ): List<CrmActivity>

    fun findByOrganizationIdAndRelatedOpportunityIdOrderByOccurredAtDesc(
        organizationId: String,
        relatedOpportunityId: String,
    ): List<CrmActivity>

    fun findByOrganizationIdAndRelatedContactIdOrderByOccurredAtDesc(
        organizationId: String,
        relatedContactId: String,
    ): List<CrmActivity>

    fun findByOrganizationIdAndRelatedCustomerIdOrderByOccurredAtDesc(
        organizationId: String,
        relatedCustomerId: String,
    ): List<CrmActivity>

    fun findByOrganizationIdAndOwnerUserIdAndCompletedFalseOrderByDueAtAsc(
        organizationId: String,
        ownerUserId: String,
    ): List<CrmActivity>
}
