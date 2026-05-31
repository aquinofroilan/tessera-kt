package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.SalesOrder
import com.aquinofroilan.tessera.model.SalesOrderStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SalesOrderRepository : JpaRepository<SalesOrder, String> {
    fun findByOrganizationId(organizationId: String): List<SalesOrder>

    fun findByOrganizationIdAndStatus(
        organizationId: String,
        status: SalesOrderStatus,
    ): List<SalesOrder>

    fun findByOrganizationIdAndCustomerId(
        organizationId: String,
        customerId: String,
    ): List<SalesOrder>

    fun findByOrganizationIdAndStatusAndCustomerId(
        organizationId: String,
        status: SalesOrderStatus,
        customerId: String,
    ): List<SalesOrder>

    fun countByOrganizationId(organizationId: String): Long
}
