package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.Quotation
import com.aquinofroilan.tessera.model.QuotationStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface QuotationRepository : JpaRepository<Quotation, String> {
    fun findByOrganizationId(organizationId: String): List<Quotation>

    fun findByOrganizationIdAndStatus(
        organizationId: String,
        status: QuotationStatus,
    ): List<Quotation>

    fun findByOrganizationIdAndCustomerId(
        organizationId: String,
        customerId: String,
    ): List<Quotation>

    fun findByOrganizationIdAndStatusAndCustomerId(
        organizationId: String,
        status: QuotationStatus,
        customerId: String,
    ): List<Quotation>

    fun countByOrganizationId(organizationId: String): Long
}
