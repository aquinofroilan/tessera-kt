package com.aquinofroilan.tessera.repository

import java.util.UUID

import com.aquinofroilan.tessera.model.ProductVariant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface ProductVariantRepository : JpaRepository<ProductVariant, UUID> {
    fun findByOrganizationIdAndProductId(
        organizationId: UUID,
        productId: UUID,
    ): List<ProductVariant>

    fun findByProductIdAndCode(
        productId: UUID,
        code: String,
    ): Optional<ProductVariant>
}
