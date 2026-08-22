package com.aquinofroilan.tessera.domain.sales.repository

import com.aquinofroilan.tessera.domain.sales.model.Quotation
import com.aquinofroilan.tessera.domain.sales.model.QuotationStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface QuotationRepository : JpaRepository<Quotation, java.util.UUID> {
    fun findByOrganizationId(organizationId: java.util.UUID): List<Quotation>

    fun findByOrganizationIdAndStatus(
        organizationId: java.util.UUID,
        status: QuotationStatus,
    ): List<Quotation>

    fun findByOrganizationIdAndCustomerId(
        organizationId: java.util.UUID,
        customerId: java.util.UUID,
    ): List<Quotation>

    fun findByOrganizationIdAndStatusAndCustomerId(
        organizationId: java.util.UUID,
        status: QuotationStatus,
        customerId: java.util.UUID,
    ): List<Quotation>

    fun countByOrganizationId(organizationId: java.util.UUID): Long
}
