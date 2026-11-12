package com.aquinofroilan.tessera.domain.inventory.repository

import com.aquinofroilan.tessera.domain.inventory.model.ProductVariant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface ProductVariantRepository : JpaRepository<ProductVariant, UUID> {
    @Query(
        """
        SELECT v FROM ProductVariant v
        WHERE v.organizationId = :organizationId
          AND (:productId IS NULL OR v.productId = :productId)
          AND (:isActive IS NULL OR v.isActive = :isActive)
        ORDER BY v.productId, v.code
        """,
    )
    fun findScopedVariants(
        @Param("organizationId") organizationId: UUID,
        @Param("productId") productId: UUID?,
        @Param("isActive") isActive: Boolean?,
    ): List<ProductVariant>

    fun findByOrganizationIdAndProductId(
        organizationId: UUID,
        productId: UUID,
    ): List<ProductVariant>

    fun findByProductIdAndCode(
        productId: UUID,
        code: String,
    ): Optional<ProductVariant>
}
