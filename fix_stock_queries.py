import sys
content = open(sys.argv[1]).read()

interface_hold = """
    fun applyHold(
        organizationId: java.util.UUID,
        productId: java.util.UUID,
        warehouseId: java.util.UUID,
        lotNumber: String? = null,
        delta: java.math.BigDecimal,
    ): Boolean
"""

# Insert interface method before `fun get(`
content = content.replace("    fun get(", interface_hold.lstrip() + "\n    fun get(")

# Replace `AND quantity + ? >= 0` with `AND quantity + ? >= held_quantity`
content = content.replace("AND quantity + ? >= 0", "AND quantity + ? >= held_quantity")

impl_hold = """
    override fun applyHold(
        organizationId: java.util.UUID,
        productId: java.util.UUID,
        warehouseId: java.util.UUID,
        lotNumber: String?,
        delta: java.math.BigDecimal,
    ): Boolean {
        if (delta.signum() == 0) return true
        val resolvedLot = lotNumber ?: ""

        if (delta.signum() > 0) {
            // Increasing hold -> ensure enough available stock
            val sql =
                \"\"\"
                UPDATE stock_on_hand
                   SET held_quantity = held_quantity + ?,
                       updated_at = current_timestamp
                 WHERE organization_id = ?::uuid
                   AND product_id = ?::uuid
                   AND warehouse_id = ?::uuid
                   AND lot_number = ?
                   AND quantity - (held_quantity + ?) >= 0
                \"\"\"
            val updated = jdbcTemplate.update(sql, delta, organizationId, productId, warehouseId, resolvedLot, delta)
            return updated > 0
        } else {
            // Decreasing hold -> just subtract
            val sql =
                \"\"\"
                UPDATE stock_on_hand
                   SET held_quantity = held_quantity + ?,
                       updated_at = current_timestamp
                 WHERE organization_id = ?::uuid
                   AND product_id = ?::uuid
                   AND warehouse_id = ?::uuid
                   AND lot_number = ?
                   AND held_quantity + ? >= 0
                \"\"\"
            val updated = jdbcTemplate.update(sql, delta, organizationId, productId, warehouseId, resolvedLot, delta)
            return updated > 0
        }
    }
"""

content = content.replace("    override fun get(", impl_hold.lstrip() + "\n    override fun get(")

open(sys.argv[1], 'w').write(content)
