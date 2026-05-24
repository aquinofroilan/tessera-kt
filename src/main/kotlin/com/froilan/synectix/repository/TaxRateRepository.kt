package com.froilan.synectix.repository

import com.froilan.synectix.model.TaxRate
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TaxRateRepository : JpaRepository<TaxRate, String> {
    fun findByOrganizationId(organizationId: String): List<TaxRate>

    fun findByOrganizationIdAndIsActive(
        organizationId: String,
        isActive: Boolean,
    ): List<TaxRate>
}
