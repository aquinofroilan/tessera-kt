package com.aquinofroilan.tessera.domain.inventory.repository

import com.aquinofroilan.tessera.domain.inventory.model.ProductVariant
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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
          AND EXISTS (
              SELECT p.id FROM Product p
              WHERE p.id = v.productId
                AND p.organizationId = :organizationId
                AND (:categoryId IS NULL OR p.categoryId = :categoryId)
                AND (
                    :search IS NULL
                    OR LOWER(v.code) LIKE CONCAT('%', LOWER(:search), '%')
                    OR LOWER(v.name) LIKE CONCAT('%', LOWER(:search), '%')
                    OR LOWER(COALESCE(v.skuSuffix, '')) LIKE CONCAT('%', LOWER(:search), '%')
                    OR LOWER(p.name) LIKE CONCAT('%', LOWER(:search), '%')
                    OR LOWER(p.sku) LIKE CONCAT('%', LOWER(:search), '%')
                )
          )
        ORDER BY v.productId, v.code, v.id
        """,
    )
    fun findOrganizationVariantsFiltered(
        @Param("organizationId") organizationId: UUID,
        @Param("productId") productId: UUID?,
        @Param("categoryId") categoryId: UUID?,
        @Param("isActive") isActive: Boolean?,
        @Param("search") search: String?,
    ): List<ProductVariant>

    @Query(
        value = """
        SELECT v FROM ProductVariant v
        WHERE v.organizationId = :organizationId
          AND (:productId IS NULL OR v.productId = :productId)
          AND (:isActive IS NULL OR v.isActive = :isActive)
          AND EXISTS (
              SELECT p.id FROM Product p
              WHERE p.id = v.productId
                AND p.organizationId = :organizationId
                AND (:categoryId IS NULL OR p.categoryId = :categoryId)
                AND (
                    :search IS NULL
                    OR LOWER(v.code) LIKE CONCAT('%', LOWER(:search), '%')
                    OR LOWER(v.name) LIKE CONCAT('%', LOWER(:search), '%')
                    OR LOWER(COALESCE(v.skuSuffix, '')) LIKE CONCAT('%', LOWER(:search), '%')
                    OR LOWER(p.name) LIKE CONCAT('%', LOWER(:search), '%')
                    OR LOWER(p.sku) LIKE CONCAT('%', LOWER(:search), '%')
                )
          )
        """,
        countQuery = """
        SELECT COUNT(v) FROM ProductVariant v
        WHERE v.organizationId = :organizationId
          AND (:productId IS NULL OR v.productId = :productId)
          AND (:isActive IS NULL OR v.isActive = :isActive)
          AND EXISTS (
              SELECT p.id FROM Product p
              WHERE p.id = v.productId
                AND p.organizationId = :organizationId
                AND (:categoryId IS NULL OR p.categoryId = :categoryId)
                AND (
                    :search IS NULL
                    OR LOWER(v.code) LIKE CONCAT('%', LOWER(:search), '%')
                    OR LOWER(v.name) LIKE CONCAT('%', LOWER(:search), '%')
                    OR LOWER(COALESCE(v.skuSuffix, '')) LIKE CONCAT('%', LOWER(:search), '%')
                    OR LOWER(p.name) LIKE CONCAT('%', LOWER(:search), '%')
                    OR LOWER(p.sku) LIKE CONCAT('%', LOWER(:search), '%')
                )
          )
        """,
    )
    fun findOrganizationVariantsFilteredPage(
        @Param("organizationId") organizationId: UUID,
        @Param("productId") productId: UUID?,
        @Param("categoryId") categoryId: UUID?,
        @Param("isActive") isActive: Boolean?,
        @Param("search") search: String?,
        pageable: Pageable,
    ): Page<ProductVariant>

    @Query(
        """
        SELECT v FROM ProductVariant v
        WHERE v.organizationId = :organizationId
          AND (:productId IS NULL OR v.productId = :productId)
          AND (:isActive IS NULL OR v.isActive = :isActive)
        ORDER BY v.productId, v.code, v.id
        """,
    )
    fun findScopedVariants(
        @Param("organizationId") organizationId: UUID,
        @Param("productId") productId: UUID?,
        @Param("isActive") isActive: Boolean?,
    ): List<ProductVariant>

    @Query(
        """
        SELECT v FROM ProductVariant v
        WHERE v.organizationId = :organizationId
          AND (:productId IS NULL OR v.productId = :productId)
          AND (:isActive IS NULL OR v.isActive = :isActive)
        """,
    )
    fun findScopedVariantsPage(
        @Param("organizationId") organizationId: UUID,
        @Param("productId") productId: UUID?,
        @Param("isActive") isActive: Boolean?,
        pageable: Pageable,
    ): Page<ProductVariant>

    fun findByOrganizationIdAndProductIdOrderByCodeAscIdAsc(
        organizationId: UUID,
        productId: UUID,
    ): List<ProductVariant>

    fun findByOrganizationIdAndProductId(
        organizationId: UUID,
        productId: UUID,
        pageable: Pageable,
    ): Page<ProductVariant>

    fun findByProductIdAndCode(
        productId: UUID,
        code: String,
    ): Optional<ProductVariant>
}
