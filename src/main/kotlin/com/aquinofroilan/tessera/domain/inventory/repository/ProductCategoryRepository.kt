package com.aquinofroilan.tessera.domain.inventory.repository

import com.aquinofroilan.tessera.domain.inventory.model.ProductCategory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProductCategoryRepository : JpaRepository<ProductCategory, UUID> {
    fun findByOrganizationIdOrderByNameAscIdAsc(organizationId: UUID): List<ProductCategory>

    fun findByOrganizationId(
        organizationId: UUID,
        pageable: Pageable,
    ): Page<ProductCategory>

    fun findByOrganizationIdAndIsActiveOrderByNameAscIdAsc(
        organizationId: UUID,
        isActive: Boolean,
    ): List<ProductCategory>

    fun findByOrganizationIdAndIsActive(
        organizationId: UUID,
        isActive: Boolean,
        pageable: Pageable,
    ): Page<ProductCategory>

    fun findByOrganizationIdAndNameIgnoreCase(
        organizationId: UUID,
        name: String,
    ): ProductCategory?
}
