package com.aquinofroilan.tessera.domain.inventory.repository

import com.aquinofroilan.tessera.domain.inventory.model.StockMovement
import com.aquinofroilan.tessera.domain.inventory.model.StockMovementType
import jakarta.persistence.EntityManager
import org.springframework.jdbc.core.JdbcTemplate
import java.math.BigDecimal
import java.time.LocalDateTime

data class OnHandKey(
    val productId: java.util.UUID,
    val warehouseId: java.util.UUID,
)

interface StockMovementQueries {
    fun listMovements(
        organizationId: java.util.UUID,
        productId: java.util.UUID?,
        warehouseId: java.util.UUID?,
        type: StockMovementType?,
        from: LocalDateTime?,
        to: LocalDateTime?,
    ): List<StockMovement>

    fun onHand(
        organizationId: java.util.UUID,
        productId: java.util.UUID,
        warehouseId: java.util.UUID,
    ): BigDecimal

    fun onHandByProductWarehouse(organizationId: java.util.UUID): Map<OnHandKey, BigDecimal>
}

open class StockMovementQueriesImpl(
    private val em: EntityManager,
    private val jdbc: JdbcTemplate,
) : StockMovementQueries {
    override fun listMovements(
        organizationId: java.util.UUID,
        productId: java.util.UUID?,
        warehouseId: java.util.UUID?,
        type: StockMovementType?,
        from: LocalDateTime?,
        to: LocalDateTime?,
    ): List<StockMovement> {
        val jpql =
            buildString {
                append("SELECT m FROM StockMovement m WHERE m.organizationId = :orgId")
                if (productId != null) append(" AND m.productId = :productId")
                if (warehouseId != null) {
                    append(" AND (m.warehouseId = :warehouseId OR m.transferToWarehouseId = :warehouseId)")
                }
                if (type != null) append(" AND m.type = :type")
                if (from != null) append(" AND m.occurredAt >= :from")
                if (to != null) append(" AND m.occurredAt <= :to")
                append(" ORDER BY m.occurredAt DESC")
            }
        val query = em.createQuery(jpql, StockMovement::class.java).setParameter("orgId", organizationId)
        if (productId != null) query.setParameter("productId", productId)
        if (warehouseId != null) query.setParameter("warehouseId", warehouseId)
        if (type != null) query.setParameter("type", type)
        if (from != null) query.setParameter("from", from)
        if (to != null) query.setParameter("to", to)
        return query.resultList
    }

    override fun onHand(
        organizationId: java.util.UUID,
        productId: java.util.UUID,
        warehouseId: java.util.UUID,
    ): BigDecimal {
        val sql =
            """
            SELECT COALESCE(SUM(
                CASE
                    WHEN type = 'TRANSFER' AND warehouse_id = ?::uuid THEN -quantity
                    WHEN type = 'TRANSFER' AND transfer_to_warehouse_id = ?::uuid THEN quantity
                    WHEN type IN ('RECEIPT', 'OPENING_BALANCE') AND warehouse_id = ?::uuid THEN quantity
                    WHEN type = 'ISSUE' AND warehouse_id = ?::uuid THEN -quantity
                    WHEN type = 'ADJUSTMENT' AND warehouse_id = ?::uuid THEN quantity
                    ELSE 0
                END
            ), 0)
            FROM stock_movements
            WHERE organization_id = ?::uuid
              AND product_id = ?::uuid
              AND (warehouse_id = ?::uuid OR transfer_to_warehouse_id = ?::uuid)
            """.trimIndent()
        return jdbc.queryForObject(
            sql,
            BigDecimal::class.java,
            warehouseId,
            warehouseId,
            warehouseId,
            warehouseId,
            warehouseId,
            organizationId,
            productId,
            warehouseId,
            warehouseId,
        ) ?: BigDecimal.ZERO
    }

    override fun onHandByProductWarehouse(organizationId: java.util.UUID): Map<OnHandKey, BigDecimal> {
        val sql =
            """
            SELECT product_id, warehouse_id, COALESCE(SUM(qty), 0) AS total_qty
            FROM (
                SELECT
                    product_id,
                    warehouse_id,
                    CASE
                        WHEN type IN ('RECEIPT', 'OPENING_BALANCE') THEN quantity
                        WHEN type IN ('ISSUE', 'TRANSFER') THEN -quantity
                        WHEN type = 'ADJUSTMENT' THEN quantity
                        ELSE 0
                    END AS qty
                FROM stock_movements
                WHERE organization_id = ?::uuid

                UNION ALL

                SELECT
                    product_id,
                    transfer_to_warehouse_id AS warehouse_id,
                    quantity AS qty
                FROM stock_movements
                WHERE organization_id = ?::uuid
                  AND type = 'TRANSFER'
                  AND transfer_to_warehouse_id IS NOT NULL
            ) AS m
            GROUP BY product_id, warehouse_id
            """.trimIndent()
        val totals = mutableMapOf<OnHandKey, BigDecimal>()
        jdbc.query(sql, { rs ->
            val productId = java.util.UUID.fromString(rs.getString("product_id"))
            val warehouseId = java.util.UUID.fromString(rs.getString("warehouse_id"))
            val qty = rs.getBigDecimal("total_qty") ?: BigDecimal.ZERO
            totals[OnHandKey(productId, warehouseId)] = qty
        }, organizationId, organizationId)
        return totals
    }
}
