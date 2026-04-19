package com.froilan.synectix.repository

import com.froilan.synectix.model.TaxRate
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface TaxRateRepository : MongoRepository<TaxRate, String> {
    fun findByOrganizationId(organizationId: String): List<TaxRate>

    fun findByOrganizationIdAndIsActive(
        organizationId: String,
        isActive: Boolean,
    ): List<TaxRate>
}
