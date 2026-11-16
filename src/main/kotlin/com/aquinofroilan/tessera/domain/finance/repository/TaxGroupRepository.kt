package com.aquinofroilan.tessera.domain.finance.repository

import com.aquinofroilan.tessera.domain.finance.model.TaxGroup
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TaxGroupRepository : JpaRepository<TaxGroup, java.util.UUID> {
    fun findByOrganizationId(organizationId: java.util.UUID): List<TaxGroup>

    fun findByOrganizationId(
        organizationId: java.util.UUID,
        pageable: Pageable,
    ): Page<TaxGroup>

    fun findByOrganizationIdAndIsActive(
        organizationId: java.util.UUID,
        isActive: Boolean,
    ): List<TaxGroup>

    fun findByOrganizationIdAndIsActive(
        organizationId: java.util.UUID,
        isActive: Boolean,
        pageable: Pageable,
    ): Page<TaxGroup>

    fun findByOrganizationIdAndTaxRateIdsContaining(
        organizationId: java.util.UUID,
        taxRateId: java.util.UUID,
    ): List<TaxGroup>
}
