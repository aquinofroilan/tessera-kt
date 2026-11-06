package com.aquinofroilan.tessera.domain.inventory.repository

import com.aquinofroilan.tessera.domain.inventory.model.Product
import jakarta.persistence.EntityManager

interface ProductQueries {
    fun search(
        organizationId: java.util.UUID,
        isActive: Boolean,
        categoryId: java.util.UUID?,
        term: String?,
    ): List<Product>
}

open class ProductQueriesImpl(
    private val em: EntityManager,
) : ProductQueries {
    override fun search(
        organizationId: java.util.UUID,
        isActive: Boolean,
        categoryId: java.util.UUID?,
        term: String?,
    ): List<Product> {
        val cleaned = term?.trim()?.takeIf { it.isNotEmpty() }
        val jpql =
            buildString {
                append("SELECT p FROM Product p WHERE p.organizationId = :orgId AND p.isActive = :active")
                if (categoryId != null) {
                    append(" AND p.categoryId = :categoryId")
                }
                if (cleaned != null) {
                    append(" AND (LOWER(p.sku) LIKE :q OR LOWER(p.name) LIKE :q)")
                }
                append(" ORDER BY p.sku ASC")
            }
        val query =
            em
                .createQuery(jpql, Product::class.java)
                .setParameter("orgId", organizationId)
                .setParameter("active", isActive)
        if (categoryId != null) {
            query.setParameter("categoryId", categoryId)
        }
        if (cleaned != null) {
            query.setParameter("q", "%${cleaned.lowercase()}%")
        }
        return query.resultList
    }
}
