package com.froilan.synectix.repository

import com.froilan.synectix.model.Vendor
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface VendorRepository : MongoRepository<Vendor, String> {
    fun findByOrganizationIdAndIsActive(
        organizationId: String,
        isActive: Boolean,
    ): List<Vendor>
}
