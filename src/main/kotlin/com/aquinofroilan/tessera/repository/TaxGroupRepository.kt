package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.TaxGroup
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TaxGroupRepository : JpaRepository<TaxGroup, String> {
    fun findByOrganizationId(organizationId: String): List<TaxGroup>

    fun findByOrganizationIdAndIsActive(
        organizationId: String,
        isActive: Boolean,
    ): List<TaxGroup>

    fun findByOrganizationIdAndTaxRateIdsContaining(
        organizationId: String,
        taxRateId: String,
    ): List<TaxGroup>
}
