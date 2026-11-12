package com.aquinofroilan.tessera.domain.inventory.repository

import com.aquinofroilan.tessera.domain.inventory.model.ProductCategory
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProductCategoryRepository : JpaRepository<ProductCategory, UUID> {
    fun findByOrganizationId(organizationId: UUID): List<ProductCategory>

    fun findByOrganizationIdAndIsActive(
        organizationId: UUID,
        isActive: Boolean,
    ): List<ProductCategory>

    fun findByOrganizationIdAndNameIgnoreCase(
        organizationId: UUID,
        name: String,
    ): ProductCategory?
}
