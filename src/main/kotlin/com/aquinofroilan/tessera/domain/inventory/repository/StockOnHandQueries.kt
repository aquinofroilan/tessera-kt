package com.aquinofroilan.tessera.domain.inventory.repository

import com.aquinofroilan.tessera.domain.inventory.dto.LotOnHandResponse
import org.springframework.jdbc.core.JdbcTemplate
import java.math.BigDecimal
import java.util.UUID

interface StockOnHandQueries {
    fun applyDelta(
        organizationId: java.util.UUID,
        productId: java.util.UUID,
        warehouseId: java.util.UUID,
        locationId: java.util.UUID? = null,
        lotNumber: String? = null,
        delta: BigDecimal,
        allowNegative: Boolean,
    ): Boolean

    fun get(
        organizationId: java.util.UUID,
        productId: java.util.UUID,
        warehouseId: java.util.UUID,
        lotNumber: String? = null,
    ): BigDecimal

    fun getLotBreakdown(
        organizationId: java.util.UUID,
        productId: java.util.UUID,
        warehouseId: java.util.UUID,
    ): List<LotOnHandResponse>

    fun getPickingSuggestions(
        organizationId: java.util.UUID,
        productId: java.util.UUID,
        warehouseId: java.util.UUID,
        requestedQuantity: BigDecimal,
    ): List<LotOnHandResponse>

    fun getExpiryReport(
        organizationId: java.util.UUID,
        daysUntilExpiry: Int,
    ): List<com.aquinofroilan.tessera.domain.inventory.dto.ExpiryReportLineResponse>
}

open class StockOnHandQueriesImpl(
    private val jdbc: JdbcTemplate,
) : StockOnHandQueries {
    override fun applyDelta(
        organizationId: java.util.UUID,
        productId: java.util.UUID,
        warehouseId: java.util.UUID,
        locationId: java.util.UUID?,
        lotNumber: String?,
        delta: BigDecimal,
        allowNegative: Boolean,
    ): Boolean {
        if (delta.signum() == 0) return true
        val resolvedLot = lotNumber ?: ""

        if (!allowNegative && delta.signum() < 0) {
            val sql =
                """
                UPDATE stock_on_hand
                   SET quantity = quantity + ?,
                       updated_at = current_timestamp
                 WHERE organization_id = ?::uuid
                   AND product_id = ?::uuid
                   AND warehouse_id = ?::uuid
                   AND COALESCE(location_id, '00000000-0000-0000-0000-000000000000'::uuid) = COALESCE(?::uuid, '00000000-0000-0000-0000-000000000000'::uuid)
                   AND lot_number = ?
                   AND quantity >= ?
                """.trimIndent()
            val updated = jdbc.update(sql, delta, organizationId, productId, warehouseId, locationId, resolvedLot, delta.negate())
            return updated > 0
        }

        val sql =
            """
            INSERT INTO stock_on_hand (id, organization_id, product_id, warehouse_id, location_id, lot_number, quantity, created_at, updated_at)
            VALUES (?::uuid, ?::uuid, ?::uuid, ?::uuid, ?::uuid, ?, ?, current_timestamp, current_timestamp)
            ON CONFLICT (organization_id, product_id, warehouse_id, COALESCE(location_id, '00000000-0000-0000-0000-000000000000'::uuid), lot_number)
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
                locationId,
                resolvedLot,
                delta,
            )
        return updated > 0
    }

    override fun get(
        organizationId: java.util.UUID,
        productId: java.util.UUID,
        warehouseId: java.util.UUID,
        lotNumber: String?,
    ): BigDecimal =
        if (lotNumber != null) {
            val sql =
                """
                SELECT quantity FROM stock_on_hand
                 WHERE organization_id = ?::uuid
                   AND product_id = ?::uuid
                   AND warehouse_id = ?::uuid
                   AND lot_number = ?
                """.trimIndent()
            val rows = jdbc.queryForList(sql, BigDecimal::class.java, organizationId, productId, warehouseId, lotNumber)
            rows.firstOrNull() ?: BigDecimal.ZERO
        } else {
            val sql =
                """
                SELECT COALESCE(SUM(quantity), 0) FROM stock_on_hand
                 WHERE organization_id = ?::uuid
                   AND product_id = ?::uuid
                   AND warehouse_id = ?::uuid
                """.trimIndent()
            val rows = jdbc.queryForList(sql, BigDecimal::class.java, organizationId, productId, warehouseId)
            rows.firstOrNull() ?: BigDecimal.ZERO
        }

    override fun getLotBreakdown(
        organizationId: java.util.UUID,
        productId: java.util.UUID,
        warehouseId: java.util.UUID,
    ): List<LotOnHandResponse> {
        val sql =
            """
            SELECT lot_number, quantity FROM stock_on_hand
             WHERE organization_id = ?::uuid
               AND product_id = ?::uuid
               AND warehouse_id = ?::uuid
             ORDER BY lot_number ASC
            """.trimIndent()
        return jdbc.query(sql, { rs, _ ->
            LotOnHandResponse(
                productId = productId,
                warehouseId = warehouseId,
                lotNumber = rs.getString("lot_number").takeIf { it.isNotEmpty() },
                quantity = rs.getBigDecimal("quantity"),
            )
        }, organizationId, productId, warehouseId)
    }

    override fun getPickingSuggestions(
        organizationId: java.util.UUID,
        productId: java.util.UUID,
        warehouseId: java.util.UUID,
        requestedQuantity: BigDecimal,
    ): List<LotOnHandResponse> {
        val sql =
            """
            SELECT s.lot_number, s.quantity, l.expiry_date
              FROM stock_on_hand s
              LEFT JOIN product_lots l 
                ON l.organization_id = s.organization_id 
               AND l.product_id = s.product_id 
               AND l.lot_number = s.lot_number
             WHERE s.organization_id = ?::uuid
               AND s.product_id = ?::uuid
               AND s.warehouse_id = ?::uuid
               AND s.quantity > 0
             ORDER BY l.expiry_date ASC NULLS LAST, s.lot_number ASC
            """.trimIndent()

        val rows =
            jdbc.query(sql, { rs, _ ->
                LotOnHandResponse(
                    productId = productId,
                    warehouseId = warehouseId,
                    lotNumber = rs.getString("lot_number").takeIf { it.isNotEmpty() },
                    quantity = rs.getBigDecimal("quantity"),
                )
            }, organizationId, productId, warehouseId)

        var remaining = requestedQuantity
        val suggestions = mutableListOf<LotOnHandResponse>()
        for (row in rows) {
            if (remaining <= BigDecimal.ZERO) break
            val toTake = row.quantity.min(remaining)
            suggestions.add(row.copy(quantity = toTake))
            remaining -= toTake
        }
        return suggestions
    }

    override fun getExpiryReport(
        organizationId: java.util.UUID,
        daysUntilExpiry: Int,
    ): List<com.aquinofroilan.tessera.domain.inventory.dto.ExpiryReportLineResponse> {
        val targetDate =
            java.time.LocalDate
                .now()
                .plusDays(daysUntilExpiry.toLong())
        val sql =
            """
            SELECT s.product_id, s.warehouse_id, s.lot_number, s.quantity, l.expiry_date
              FROM stock_on_hand s
              JOIN product_lots l 
                ON l.organization_id = s.organization_id 
               AND l.product_id = s.product_id 
               AND l.lot_number = s.lot_number
             WHERE s.organization_id = ?::uuid
               AND s.quantity > 0
               AND l.expiry_date <= ?
             ORDER BY l.expiry_date ASC, s.product_id ASC
            """.trimIndent()

        return jdbc.query(sql, { rs, _ ->
            com.aquinofroilan.tessera.domain.inventory.dto.ExpiryReportLineResponse(
                productId = rs.getObject("product_id", java.util.UUID::class.java),
                warehouseId = rs.getObject("warehouse_id", java.util.UUID::class.java),
                lotNumber = rs.getString("lot_number"),
                quantity = rs.getBigDecimal("quantity"),
                expiryDate = rs.getObject("expiry_date", java.time.LocalDate::class.java),
            )
        }, organizationId, targetDate)
    }
}
