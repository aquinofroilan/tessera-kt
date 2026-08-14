package com.aquinofroilan.tessera.domain.procurement.repository

import com.aquinofroilan.tessera.domain.procurement.model.PurchaseRequest
import com.aquinofroilan.tessera.domain.procurement.model.PurchaseRequestStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PurchaseRequestRepository : JpaRepository<PurchaseRequest, java.util.UUID> {
    fun findByOrganizationId(organizationId: java.util.UUID): List<PurchaseRequest>

    fun findByOrganizationIdAndStatus(
        organizationId: java.util.UUID,
        status: PurchaseRequestStatus,
    ): List<PurchaseRequest>

    fun findByOrganizationIdAndRequestedBy(
        organizationId: java.util.UUID,
        requestedBy: java.util.UUID,
    ): List<PurchaseRequest>

    fun countByOrganizationId(organizationId: java.util.UUID): Long
}
