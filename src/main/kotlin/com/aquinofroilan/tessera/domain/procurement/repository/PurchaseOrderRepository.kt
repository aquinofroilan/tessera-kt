package com.aquinofroilan.tessera.domain.procurement.repository

import com.aquinofroilan.tessera.domain.procurement.model.PurchaseOrder
import com.aquinofroilan.tessera.domain.procurement.model.PurchaseOrderStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PurchaseOrderRepository : JpaRepository<PurchaseOrder, java.util.UUID> {
    fun findByOrganizationId(organizationId: java.util.UUID): List<PurchaseOrder>

    fun findByOrganizationIdAndStatus(
        organizationId: java.util.UUID,
        status: PurchaseOrderStatus,
    ): List<PurchaseOrder>

    fun findByOrganizationIdAndVendorId(
        organizationId: java.util.UUID,
        vendorId: java.util.UUID,
    ): List<PurchaseOrder>

    fun findByOrganizationIdAndStatusAndVendorId(
        organizationId: java.util.UUID,
        status: PurchaseOrderStatus,
        vendorId: java.util.UUID,
    ): List<PurchaseOrder>

    fun countByOrganizationId(organizationId: java.util.UUID): Long
}
