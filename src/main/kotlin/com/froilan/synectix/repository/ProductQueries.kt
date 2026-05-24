package com.froilan.synectix.repository

import com.froilan.synectix.model.Product
import jakarta.persistence.EntityManager

interface ProductQueries {
    fun search(
        organizationId: String,
        isActive: Boolean,
        category: String?,
        term: String?,
    ): List<Product>
}

open class ProductQueriesImpl(
    private val em: EntityManager,
) : ProductQueries {
    override fun search(
        organizationId: String,
        isActive: Boolean,
        category: String?,
        term: String?,
    ): List<Product> {
        val cleaned = term?.trim()?.takeIf { it.isNotEmpty() }
        val jpql =
            buildString {
                append("SELECT p FROM Product p WHERE p.organizationId = :orgId AND p.isActive = :active")
                if (category != null) {
                    append(" AND p.category = :category")
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
        if (category != null) {
            query.setParameter("category", category)
        }
        if (cleaned != null) {
            query.setParameter("q", "%${cleaned.lowercase()}%")
        }
        return query.resultList
    }
}
