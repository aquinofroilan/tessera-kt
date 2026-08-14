package com.aquinofroilan.tessera.domain.inventory.repository

import com.aquinofroilan.tessera.domain.inventory.model.ProductVariant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

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
