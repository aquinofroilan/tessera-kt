package com.aquinofroilan.tessera.domain.inventory.repository

import com.aquinofroilan.tessera.domain.inventory.model.Product
import jakarta.persistence.EntityManager
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

interface ProductQueries {
    fun search(
        organizationId: java.util.UUID,
        isActive: Boolean,
        categoryId: java.util.UUID?,
        term: String?,
    ): List<Product>

    fun search(
        organizationId: java.util.UUID,
        isActive: Boolean,
        categoryId: java.util.UUID?,
        term: String?,
        pageable: Pageable,
    ): Page<Product>
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
        val predicate =
            buildString {
                append("p.organizationId = :orgId AND p.isActive = :active")
                if (categoryId != null) {
                    append(" AND p.categoryId = :categoryId")
                }
                if (cleaned != null) {
                    append(" AND (LOWER(p.sku) LIKE :q OR LOWER(p.name) LIKE :q)")
                }
            }
        val jpql = "SELECT p FROM Product p WHERE $predicate ORDER BY p.sku ASC, p.id ASC"
        val query = em.createQuery(jpql, Product::class.java)
        bindSearchParameters(query, organizationId, isActive, categoryId, cleaned)
        return query.resultList
    }

    override fun search(
        organizationId: java.util.UUID,
        isActive: Boolean,
        categoryId: java.util.UUID?,
        term: String?,
        pageable: Pageable,
    ): Page<Product> {
        val cleaned = term?.trim()?.takeIf { it.isNotEmpty() }
        val predicate =
            buildString {
                append("p.organizationId = :orgId AND p.isActive = :active")
                if (categoryId != null) {
                    append(" AND p.categoryId = :categoryId")
                }
                if (cleaned != null) {
                    append(" AND (LOWER(p.sku) LIKE :q OR LOWER(p.name) LIKE :q)")
                }
            }
        val sort = pageable.sort
        val orderBy =
            if (sort.isUnsorted) {
                "p.sku ASC, p.id ASC"
            } else {
                sort.joinToString(", ") { order ->
                    val property = when (order.property) {
                        "sku" -> "p.sku"
                        "name" -> "p.name"
                        "id" -> "p.id"
                        else -> "p.sku"
                    }
                    "$property ${if (order.isAscending) "ASC" else "DESC"}"
                }
            }
        val query = em.createQuery("SELECT p FROM Product p WHERE $predicate ORDER BY $orderBy", Product::class.java)
        bindSearchParameters(query, organizationId, isActive, categoryId, cleaned)
        if (pageable.isPaged) {
            query.firstResult = pageable.offset.toInt()
            query.maxResults = pageable.pageSize
        }
        val products = query.resultList
        val countQuery = em.createQuery("SELECT COUNT(p) FROM Product p WHERE $predicate", Long::class.javaObjectType)
        bindSearchParameters(countQuery, organizationId, isActive, categoryId, cleaned)
        val total = countQuery.singleResult.toLong()
        return PageImpl(products, pageable, total)
    }

    private fun bindSearchParameters(
        query: jakarta.persistence.Query,
        organizationId: java.util.UUID,
        isActive: Boolean,
        categoryId: java.util.UUID?,
        cleaned: String?,
    ) {
        query.setParameter("orgId", organizationId)
        query.setParameter("active", isActive)
        if (categoryId != null) {
            query.setParameter("categoryId", categoryId)
        }
        if (cleaned != null) {
            query.setParameter("q", "%${cleaned.lowercase()}%")
        }
    }
}
