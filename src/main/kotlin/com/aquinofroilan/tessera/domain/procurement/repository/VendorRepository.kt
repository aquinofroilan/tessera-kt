package com.aquinofroilan.tessera.domain.procurement.repository

import com.aquinofroilan.tessera.domain.procurement.model.Vendor
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface VendorRepository : JpaRepository<Vendor, java.util.UUID> {
    fun findByOrganizationId(organizationId: java.util.UUID): List<Vendor>

    fun findByIdAndOrganizationId(
        id: java.util.UUID,
        organizationId: java.util.UUID,
    ): java.util.Optional<Vendor>

    fun findByOrganizationIdAndIsActive(
        organizationId: java.util.UUID,
        isActive: Boolean,
    ): List<Vendor>
}
