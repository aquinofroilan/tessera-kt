package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.WorkOrder
import com.aquinofroilan.tessera.model.WorkOrderStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface WorkOrderRepository : JpaRepository<WorkOrder, String> {
    fun findByOrganizationId(organizationId: String): List<WorkOrder>

    fun findByOrganizationIdAndStatus(
        organizationId: String,
        status: WorkOrderStatus,
    ): List<WorkOrder>

    fun findByOrganizationIdAndProductId(
        organizationId: String,
        productId: String,
    ): List<WorkOrder>

    fun countByOrganizationId(organizationId: String): Long
}
