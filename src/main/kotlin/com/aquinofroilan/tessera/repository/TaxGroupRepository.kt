package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.TaxGroup
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TaxGroupRepository : JpaRepository<TaxGroup, java.util.UUID> {
    fun findByOrganizationId(organizationId: java.util.UUID): List<TaxGroup>

    fun findByOrganizationIdAndIsActive(
        organizationId: java.util.UUID,
        isActive: Boolean,
    ): List<TaxGroup>

    fun findByOrganizationIdAndTaxRateIdsContaining(
        organizationId: java.util.UUID,
        taxRateId: java.util.UUID,
    ): List<TaxGroup>
}
