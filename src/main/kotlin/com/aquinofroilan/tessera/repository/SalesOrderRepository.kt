package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.SalesOrder
import com.aquinofroilan.tessera.model.SalesOrderStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SalesOrderRepository : JpaRepository<SalesOrder, java.util.UUID> {
    fun findByOrganizationId(organizationId: java.util.UUID): List<SalesOrder>

    fun findByOrganizationIdAndStatus(
        organizationId: java.util.UUID,
        status: SalesOrderStatus,
    ): List<SalesOrder>

    fun findByOrganizationIdAndCustomerId(
        organizationId: java.util.UUID,
        customerId: java.util.UUID,
    ): List<SalesOrder>

    fun findByOrganizationIdAndStatusAndCustomerId(
        organizationId: java.util.UUID,
        status: SalesOrderStatus,
        customerId: java.util.UUID,
    ): List<SalesOrder>

    fun countByOrganizationId(organizationId: java.util.UUID): Long
}
