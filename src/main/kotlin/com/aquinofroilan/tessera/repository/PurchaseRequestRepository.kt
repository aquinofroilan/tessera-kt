package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.PurchaseRequest
import com.aquinofroilan.tessera.model.PurchaseRequestStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PurchaseRequestRepository : JpaRepository<PurchaseRequest, String> {
    fun findByOrganizationId(organizationId: String): List<PurchaseRequest>

    fun findByOrganizationIdAndStatus(
        organizationId: String,
        status: PurchaseRequestStatus,
    ): List<PurchaseRequest>

    fun findByOrganizationIdAndRequestedBy(
        organizationId: String,
        requestedBy: String,
    ): List<PurchaseRequest>

    fun countByOrganizationId(organizationId: String): Long
}
