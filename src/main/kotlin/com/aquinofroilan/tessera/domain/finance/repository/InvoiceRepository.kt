package com.aquinofroilan.tessera.domain.finance.repository

import com.aquinofroilan.tessera.domain.finance.model.Invoice
import com.aquinofroilan.tessera.domain.finance.model.InvoiceStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface InvoiceRepository : JpaRepository<Invoice, java.util.UUID> {
    fun findByOrganizationId(organizationId: java.util.UUID): List<Invoice>

    fun findByOrganizationIdAndStatus(
        organizationId: java.util.UUID,
        status: InvoiceStatus,
    ): List<Invoice>

    fun findByOrganizationIdAndCustomerId(
        organizationId: java.util.UUID,
        customerId: java.util.UUID,
    ): List<Invoice>

    fun findByOrganizationIdAndStatusAndCustomerId(
        organizationId: java.util.UUID,
        status: InvoiceStatus,
        customerId: java.util.UUID,
    ): List<Invoice>

    fun findByOrganizationIdAndStatusIn(
        organizationId: java.util.UUID,
        statuses: List<InvoiceStatus>,
    ): List<Invoice>

    fun countByOrganizationId(organizationId: java.util.UUID): Long
}
