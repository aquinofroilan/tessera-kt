package com.aquinofroilan.tessera.domain.sales.repository

import com.aquinofroilan.tessera.domain.sales.model.Customer
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CustomerRepository : JpaRepository<Customer, java.util.UUID> {
    fun findByOrganizationId(organizationId: java.util.UUID): List<Customer>

    fun findByIdAndOrganizationId(
        id: java.util.UUID,
        organizationId: java.util.UUID,
    ): java.util.Optional<Customer>

    fun findByOrganizationIdAndIsActive(
        organizationId: java.util.UUID,
        isActive: Boolean,
    ): List<Customer>
}
