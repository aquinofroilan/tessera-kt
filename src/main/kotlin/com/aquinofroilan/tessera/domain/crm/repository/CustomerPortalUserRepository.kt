package com.aquinofroilan.tessera.domain.crm.repository

import com.aquinofroilan.tessera.domain.crm.model.CustomerPortalUser
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface CustomerPortalUserRepository : JpaRepository<CustomerPortalUser, UUID> {
    fun findByOrganizationIdAndUserId(
        organizationId: UUID,
        userId: UUID,
    ): Optional<CustomerPortalUser>

    fun findByOrganizationIdAndCustomerId(
        organizationId: UUID,
        customerId: UUID,
    ): List<CustomerPortalUser>

    fun findByOrganizationIdAndCustomerIdAndUserId(
        organizationId: UUID,
        customerId: UUID,
        userId: UUID,
    ): Optional<CustomerPortalUser>

    fun existsByOrganizationIdAndUserId(
        organizationId: UUID,
        userId: UUID,
    ): Boolean

    fun deleteByOrganizationIdAndCustomerIdAndUserId(
        organizationId: UUID,
        customerId: UUID,
        userId: UUID,
    )
}
