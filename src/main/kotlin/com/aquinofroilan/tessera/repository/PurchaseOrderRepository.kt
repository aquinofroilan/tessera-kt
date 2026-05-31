package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.PurchaseOrder
import com.aquinofroilan.tessera.model.PurchaseOrderStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PurchaseOrderRepository : JpaRepository<PurchaseOrder, String> {
    fun findByOrganizationId(organizationId: String): List<PurchaseOrder>

    fun findByOrganizationIdAndStatus(
        organizationId: String,
        status: PurchaseOrderStatus,
    ): List<PurchaseOrder>

    fun findByOrganizationIdAndVendorId(
        organizationId: String,
        vendorId: String,
    ): List<PurchaseOrder>

    fun findByOrganizationIdAndStatusAndVendorId(
        organizationId: String,
        status: PurchaseOrderStatus,
        vendorId: String,
    ): List<PurchaseOrder>

    fun countByOrganizationId(organizationId: String): Long
}
