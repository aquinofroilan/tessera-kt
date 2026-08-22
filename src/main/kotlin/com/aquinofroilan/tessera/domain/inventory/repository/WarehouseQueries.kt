package com.aquinofroilan.tessera.domain.inventory.repository

import com.aquinofroilan.tessera.domain.inventory.model.Warehouse
import jakarta.persistence.EntityManager

interface WarehouseQueries {
    fun search(
        organizationId: java.util.UUID,
        isActive: Boolean,
        term: String?,
    ): List<Warehouse>
}

open class WarehouseQueriesImpl(
    private val em: EntityManager,
) : WarehouseQueries {
    override fun search(
        organizationId: java.util.UUID,
        isActive: Boolean,
        term: String?,
    ): List<Warehouse> {
        val cleaned = term?.trim()?.takeIf { it.isNotEmpty() }
        val jpql =
            buildString {
                append("SELECT w FROM Warehouse w WHERE w.organizationId = :orgId AND w.isActive = :active")
                if (cleaned != null) {
                    append(" AND (LOWER(w.code) LIKE :q OR LOWER(w.name) LIKE :q)")
                }
                append(" ORDER BY w.code ASC")
            }
        val query =
            em
                .createQuery(jpql, Warehouse::class.java)
                .setParameter("orgId", organizationId)
                .setParameter("active", isActive)
        if (cleaned != null) {
            query.setParameter("q", "%${cleaned.lowercase()}%")
        }
        return query.resultList
    }
}
