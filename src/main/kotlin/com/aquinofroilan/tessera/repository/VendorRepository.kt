package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.Vendor
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface VendorRepository : JpaRepository<Vendor, java.util.UUID> {
    fun findByOrganizationIdAndIsActive(
        organizationId: java.util.UUID,
        isActive: Boolean,
    ): List<Vendor>
}
