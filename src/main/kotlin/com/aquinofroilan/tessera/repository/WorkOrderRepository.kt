package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.WorkOrder
import com.aquinofroilan.tessera.model.WorkOrderStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface WorkOrderRepository : JpaRepository<WorkOrder, java.util.UUID> {
    fun findByOrganizationId(organizationId: java.util.UUID): List<WorkOrder>

    fun findByOrganizationIdAndStatus(
        organizationId: java.util.UUID,
        status: WorkOrderStatus,
    ): List<WorkOrder>

    fun findByOrganizationIdAndProductId(
        organizationId: java.util.UUID,
        productId: java.util.UUID,
    ): List<WorkOrder>

    fun countByOrganizationId(organizationId: java.util.UUID): Long
}
