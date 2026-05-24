package com.froilan.synectix.repository

import com.froilan.synectix.model.StockOnHand
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import java.math.BigDecimal

interface StockOnHandQueries {
    /**
     * Atomically apply [delta] to the (organizationId, productId, warehouseId) counter.
     *
     * Returns true on success, false when [allowNegative] is false and the post-state
     * would be negative (insufficient stock). All concurrency-relevant checks happen
     * inside a single MongoDB updateFirst, so two callers cannot both succeed past a
     * negative-stock boundary.
     */
    fun applyDelta(
        organizationId: String,
        productId: String,
        warehouseId: String,
        delta: BigDecimal,
        allowNegative: Boolean,
    ): Boolean

    fun get(
        organizationId: String,
        productId: String,
        warehouseId: String,
    ): BigDecimal
}

open class StockOnHandQueriesImpl(
    private val mongoTemplate: MongoTemplate,
) : StockOnHandQueries {
    override fun applyDelta(
        organizationId: String,
        productId: String,
        warehouseId: String,
        delta: BigDecimal,
        allowNegative: Boolean,
    ): Boolean {
        if (delta.signum() == 0) return true

        val keyCriteria =
            Criteria
                .where("organizationId")
                .`is`(organizationId)
                .and("productId")
                .`is`(productId)
                .and("warehouseId")
                .`is`(warehouseId)

        if (!allowNegative && delta.signum() < 0) {
            val guarded =
                Query(keyCriteria)
                    .addCriteria(Criteria.where("quantity").gte(delta.negate()))
            val result =
                mongoTemplate.updateFirst(
                    guarded,
                    Update().inc("quantity", delta),
                    StockOnHand::class.java,
                )
            return result.modifiedCount > 0
        }

        var attempts = 0
        while (attempts < 3) {
            attempts++
            try {
                val result =
                    mongoTemplate.upsert(
                        Query(keyCriteria),
                        Update()
                            .inc("quantity", delta)
                            .setOnInsert("organizationId", organizationId)
                            .setOnInsert("productId", productId)
                            .setOnInsert("warehouseId", warehouseId),
                        StockOnHand::class.java,
                    )
                if (result.modifiedCount > 0 || result.upsertedId != null) return true
            } catch (_: DuplicateKeyException) {
                // Concurrent insert race; retry as a plain update against the now-existing doc.
            }
        }
        return false
    }

    override fun get(
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
                .and("warehouseId")
                .`is`(warehouseId)
        val doc = mongoTemplate.findOne(Query(criteria), StockOnHand::class.java)
        return doc?.quantity ?: BigDecimal.ZERO
    }
}
