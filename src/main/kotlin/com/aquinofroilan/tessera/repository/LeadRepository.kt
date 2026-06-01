package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.Lead
import com.aquinofroilan.tessera.model.LeadStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface LeadRepository : JpaRepository<Lead, String> {
    fun findByOrganizationId(organizationId: String): List<Lead>

    fun findByOrganizationIdAndStatus(
        organizationId: String,
        status: LeadStatus,
    ): List<Lead>

    fun findByOrganizationIdAndOwnerUserId(
        organizationId: String,
        ownerUserId: String,
    ): List<Lead>
}
