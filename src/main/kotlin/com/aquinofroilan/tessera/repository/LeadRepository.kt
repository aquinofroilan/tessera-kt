package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.Lead
import com.aquinofroilan.tessera.model.LeadStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface LeadRepository : JpaRepository<Lead, UUID> {
    fun findByOrganizationId(organizationId: UUID): List<Lead>

    fun findByOrganizationIdAndStatus(
        organizationId: UUID,
        status: LeadStatus,
    ): List<Lead>

    fun findByOrganizationIdAndOwnerUserId(
        organizationId: UUID,
        ownerUserId: UUID,
    ): List<Lead>
}
