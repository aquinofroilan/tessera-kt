package com.froilan.synectix.repository

import com.froilan.synectix.model.StockMovement
import com.froilan.synectix.model.StockMovementType
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import java.math.BigDecimal
import java.time.LocalDateTime

data class OnHandKey(
    val productId: String,
    val warehouseId: String,
)

interface StockMovementQueries {
    fun listMovements(
        organizationId: String,
        productId: String?,
        warehouseId: String?,
        type: StockMovementType?,
        from: LocalDateTime?,
        to: LocalDateTime?,
    ): List<StockMovement>

    fun onHand(
        organizationId: String,
        productId: String,
        warehouseId: String,
    ): BigDecimal

    fun onHandByProductWarehouse(organizationId: String): Map<OnHandKey, BigDecimal>
}

open class StockMovementQueriesImpl(
    private val mongoTemplate: MongoTemplate,
) : StockMovementQueries {
    override fun listMovements(
        organizationId: String,
        productId: String?,
        warehouseId: String?,
        type: StockMovementType?,
        from: LocalDateTime?,
        to: LocalDateTime?,
    ): List<StockMovement> {
        val criteria = Criteria.where("organizationId").`is`(organizationId)
        if (productId != null) criteria.and("productId").`is`(productId)
        if (warehouseId != null) {
            criteria.orOperator(
                Criteria.where("warehouseId").`is`(warehouseId),
                Criteria.where("transferToWarehouseId").`is`(warehouseId),
            )
        }
        if (type != null) criteria.and("type").`is`(type.name)
        if (from != null && to != null) {
            criteria.and("occurredAt").gte(from).lte(to)
        } else if (from != null) {
            criteria.and("occurredAt").gte(from)
        } else if (to != null) {
            criteria.and("occurredAt").lte(to)
        }
        return mongoTemplate.find(
            Query(criteria).with(Sort.by(Sort.Direction.DESC, "occurredAt")),
            StockMovement::class.java,
        )
    }

    override fun onHand(
        organizationId: String,
        productId: String,
        warehouseId: String,
    ): BigDecimal {
        val criteria =
            Criteria
                .where("organizationId")
                .`is`(organizationId)
                .and("productId")
                .`is`(productId)
                .orOperator(
                    Criteria.where("warehouseId").`is`(warehouseId),
                    Criteria.where("transferToWarehouseId").`is`(warehouseId),
                )
        val rows = mongoTemplate.find(Query(criteria), StockMovement::class.java)
        return rows.fold(BigDecimal.ZERO) { acc, m -> acc + signedQuantity(m, warehouseId) }
    }

    override fun onHandByProductWarehouse(organizationId: String): Map<OnHandKey, BigDecimal> {
        val criteria = Criteria.where("organizationId").`is`(organizationId)
        val all = mongoTemplate.find(Query(criteria), StockMovement::class.java)
        val totals = mutableMapOf<OnHandKey, BigDecimal>()
        for (m in all) {
            val primary = OnHandKey(m.productId, m.warehouseId)
            totals[primary] = (totals[primary] ?: BigDecimal.ZERO) + signedQuantity(m, m.warehouseId)
            if (m.type == StockMovementType.TRANSFER && m.transferToWarehouseId != null) {
                val dest = OnHandKey(m.productId, m.transferToWarehouseId)
                totals[dest] = (totals[dest] ?: BigDecimal.ZERO) + signedQuantity(m, m.transferToWarehouseId)
            }
        }
        return totals
    }

    private fun signedQuantity(
        movement: StockMovement,
        forWarehouseId: String,
    ): BigDecimal {
        val q = movement.quantity
        return when (movement.type) {
            StockMovementType.RECEIPT,
            StockMovementType.OPENING_BALANCE,
            -> if (movement.warehouseId == forWarehouseId) q else BigDecimal.ZERO
            StockMovementType.ISSUE ->
                if (movement.warehouseId == forWarehouseId) q.negate() else BigDecimal.ZERO
            StockMovementType.ADJUSTMENT ->
                if (movement.warehouseId == forWarehouseId) q else BigDecimal.ZERO
            StockMovementType.TRANSFER ->
                when (forWarehouseId) {
                    movement.warehouseId -> q.negate()
                    movement.transferToWarehouseId -> q
                    else -> BigDecimal.ZERO
                }
        }
    }
}
