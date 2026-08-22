package com.aquinofroilan.tessera.domain.finance.repository

import com.aquinofroilan.tessera.domain.finance.model.TaxRate
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TaxRateRepository : JpaRepository<TaxRate, java.util.UUID> {
    fun findByOrganizationId(organizationId: java.util.UUID): List<TaxRate>

    fun findByOrganizationIdAndIsActive(
        organizationId: java.util.UUID,
        isActive: Boolean,
    ): List<TaxRate>
}
