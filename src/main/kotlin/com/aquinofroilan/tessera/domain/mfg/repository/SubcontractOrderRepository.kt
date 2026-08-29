package com.aquinofroilan.tessera.domain.mfg.repository

import com.aquinofroilan.tessera.domain.mfg.model.SubcontractOrder
import com.aquinofroilan.tessera.domain.mfg.model.SubcontractOrderStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface SubcontractOrderRepository : JpaRepository<SubcontractOrder, UUID> {
    fun findByOrganizationId(organizationId: UUID): List<SubcontractOrder>

    fun findByIdAndOrganizationId(
        id: UUID,
        organizationId: UUID,
    ): Optional<SubcontractOrder>

    fun findByOrganizationIdAndWorkOrderId(
        organizationId: UUID,
        workOrderId: UUID,
    ): List<SubcontractOrder>

    fun findByOrganizationIdAndVendorId(
        organizationId: UUID,
        vendorId: UUID,
    ): List<SubcontractOrder>

    fun findByOrganizationIdAndStatus(
        organizationId: UUID,
        status: SubcontractOrderStatus,
    ): List<SubcontractOrder>

    fun findByOrganizationIdAndOrderNumber(
        organizationId: UUID,
        orderNumber: String,
    ): Optional<SubcontractOrder>

    fun countByOrganizationId(organizationId: UUID): Long
}
