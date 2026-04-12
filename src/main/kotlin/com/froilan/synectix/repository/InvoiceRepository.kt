package com.froilan.synectix.repository

import com.froilan.synectix.model.Invoice
import com.froilan.synectix.model.InvoiceStatus
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface InvoiceRepository : MongoRepository<Invoice, String> {
    fun findByOrganizationId(organizationId: String): List<Invoice>

    fun findByOrganizationIdAndStatus(
        organizationId: String,
        status: InvoiceStatus,
    ): List<Invoice>

    fun findByOrganizationIdAndCustomerId(
        organizationId: String,
        customerId: String,
    ): List<Invoice>

    fun findByOrganizationIdAndStatusAndCustomerId(
        organizationId: String,
        status: InvoiceStatus,
        customerId: String,
    ): List<Invoice>

    fun findByOrganizationIdAndStatusIn(
        organizationId: String,
        statuses: List<InvoiceStatus>,
    ): List<Invoice>

    fun countByOrganizationId(organizationId: String): Long
}
