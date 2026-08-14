package com.aquinofroilan.tessera.domain.sales.repository

import com.aquinofroilan.tessera.domain.sales.model.Customer
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CustomerRepository : JpaRepository<Customer, java.util.UUID> {
    fun findByOrganizationIdAndIsActive(
        organizationId: java.util.UUID,
        isActive: Boolean,
    ): List<Customer>
}
