package com.aquinofroilan.tessera.domain.procurement.repository

import com.aquinofroilan.tessera.domain.procurement.model.VendorEvaluation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

@Repository
interface VendorEvaluationRepository : JpaRepository<VendorEvaluation, UUID> {
    fun findByOrganizationIdAndVendorIdOrderByEvaluationDateDesc(
        organizationId: UUID,
        vendorId: UUID,
    ): List<VendorEvaluation>

    fun findByOrganizationIdAndVendorIdAndEvaluationDateBetweenOrderByEvaluationDateDesc(
        organizationId: UUID,
        vendorId: UUID,
        from: LocalDate,
        to: LocalDate,
    ): List<VendorEvaluation>

    fun findByOrganizationIdAndId(
        organizationId: UUID,
        id: UUID,
    ): Optional<VendorEvaluation>

    fun findByOrganizationId(organizationId: UUID): List<VendorEvaluation>
}
