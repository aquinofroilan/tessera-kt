package com.froilan.synectix.repository

import com.froilan.synectix.model.TaxGroup
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface TaxGroupRepository : MongoRepository<TaxGroup, String> {
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
