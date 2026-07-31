package com.aquinofroilan.tessera.repository

import org.springframework.jdbc.core.JdbcTemplate
import java.math.BigDecimal
import java.util.UUID

interface StockOnHandQueries {
    /**
     * Atomically apply [delta] to the (organizationId, productId, warehouseId) counter.
     *
     * Returns true on success, false when [allowNegative] is false and the post-state
     * would be negative (insufficient stock). The check and decrement happen in a single
     * SQL UPDATE with a row-level negative-stock guard, so concurrent callers cannot
     * both succeed past a negative-stock boundary.
     */
    fun applyDelta(
        organizationId: java.util.UUID,
        productId: java.util.UUID,
        warehouseId: java.util.UUID,
        delta: BigDecimal,
        allowNegative: Boolean,
    ): Boolean

    fun get(
        organizationId: java.util.UUID,
        productId: java.util.UUID,
        warehouseId: java.util.UUID,
    ): BigDecimal
}

open class StockOnHandQueriesImpl(
    private val jdbc: JdbcTemplate,
) : StockOnHandQueries {
    override fun applyDelta(
        organizationId: java.util.UUID,
        productId: java.util.UUID,
        warehouseId: java.util.UUID,
        delta: BigDecimal,
        allowNegative: Boolean,
    ): Boolean {
        if (delta.signum() == 0) return true

        if (!allowNegative && delta.signum() < 0) {
            // Atomic check-and-decrement on existing row only. No upsert: a missing row
            // means quantity is implicitly 0, which fails the guard for an outbound delta.
            val sql =
                """
                UPDATE stock_on_hand
                   SET quantity = quantity + ?,
                       updated_at = current_timestamp
                 WHERE organization_id = ?::uuid
                   AND product_id = ?::uuid
                   AND warehouse_id = ?::uuid
                   AND quantity >= ?
                """.trimIndent()
            val updated = jdbc.update(sql, delta, organizationId, productId, warehouseId, delta.negate())
            return updated > 0
        }

        // Inbound (or allowed-negative): upsert. Postgres ON CONFLICT handles the race natively.
        val sql =
            """
            INSERT INTO stock_on_hand (id, organization_id, product_id, warehouse_id, quantity, created_at, updated_at)
            VALUES (?::uuid, ?::uuid, ?::uuid, ?::uuid, ?, current_timestamp, current_timestamp)
            ON CONFLICT (organization_id, product_id, warehouse_id)
            DO UPDATE SET quantity = stock_on_hand.quantity + EXCLUDED.quantity,
                          updated_at = current_timestamp
            """.trimIndent()
        val updated =
            jdbc.update(
                sql,
                java.util.UUID.ofEpochMillis(System.currentTimeMillis()),
                organizationId,
                productId,
                warehouseId,
                delta,
            )
        return updated > 0
    }

    override fun get(
        organizationId: java.util.UUID,
        productId: java.util.UUID,
        warehouseId: java.util.UUID,
    ): BigDecimal {
        val sql =
            """
            SELECT quantity FROM stock_on_hand
             WHERE organization_id = ?::uuid
               AND product_id = ?::uuid
               AND warehouse_id = ?::uuid
            """.trimIndent()
        val rows = jdbc.queryForList(sql, BigDecimal::class.java, organizationId, productId, warehouseId)
        return rows.firstOrNull() ?: BigDecimal.ZERO
    }
}
