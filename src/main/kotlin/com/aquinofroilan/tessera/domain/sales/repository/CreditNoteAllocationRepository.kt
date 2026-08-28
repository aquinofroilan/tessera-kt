package com.aquinofroilan.tessera.domain.sales.repository

import com.aquinofroilan.tessera.domain.sales.model.CreditNoteAllocation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface CreditNoteAllocationRepository : JpaRepository<CreditNoteAllocation, UUID> {
    fun findByOrganizationIdAndCreditNoteId(
        organizationId: UUID,
        creditNoteId: UUID,
    ): List<CreditNoteAllocation>

    fun findByOrganizationIdAndInvoiceId(
        organizationId: UUID,
        invoiceId: UUID,
    ): List<CreditNoteAllocation>
}
