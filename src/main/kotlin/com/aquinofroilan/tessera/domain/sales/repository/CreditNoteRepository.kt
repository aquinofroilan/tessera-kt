package com.aquinofroilan.tessera.domain.sales.repository

import com.aquinofroilan.tessera.domain.sales.model.CreditNote
import com.aquinofroilan.tessera.domain.sales.model.CreditNoteStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface CreditNoteRepository : JpaRepository<CreditNote, UUID> {
    fun findByOrganizationId(organizationId: UUID): List<CreditNote>

    fun findByIdAndOrganizationId(
        id: UUID,
        organizationId: UUID,
    ): Optional<CreditNote>

    fun findByOrganizationIdAndCustomerId(
        organizationId: UUID,
        customerId: UUID,
    ): List<CreditNote>

    fun findByOrganizationIdAndStatus(
        organizationId: UUID,
        status: CreditNoteStatus,
    ): List<CreditNote>

    fun countByOrganizationId(organizationId: UUID): Long

    fun existsByOrganizationIdAndCreditNoteNumber(
        organizationId: UUID,
        creditNoteNumber: String,
    ): Boolean
}
