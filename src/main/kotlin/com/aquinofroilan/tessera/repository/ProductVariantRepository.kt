package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.ProductVariant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface ProductVariantRepository : JpaRepository<ProductVariant, String> {
    fun findByOrganizationIdAndProductId(
        organizationId: String,
        productId: String,
    ): List<ProductVariant>

    fun findByProductIdAndCode(
        productId: String,
        code: String,
    ): Optional<ProductVariant>
}
