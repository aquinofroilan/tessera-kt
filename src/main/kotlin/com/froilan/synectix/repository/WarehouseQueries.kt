package com.froilan.synectix.repository

import com.froilan.synectix.model.Warehouse
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query

interface WarehouseQueries {
    fun search(
        organizationId: String,
        isActive: Boolean,
        term: String?,
    ): List<Warehouse>
}

open class WarehouseQueriesImpl(
    private val mongoTemplate: MongoTemplate,
) : WarehouseQueries {
    override fun search(
        organizationId: String,
        isActive: Boolean,
        term: String?,
    ): List<Warehouse> {
        val criteria =
            Criteria
                .where("organizationId")
                .`is`(organizationId)
                .and("isActive")
                .`is`(isActive)
        val cleaned = term?.trim()?.takeIf { it.isNotEmpty() }
        if (cleaned != null) {
            val escaped = Regex.escape(cleaned)
            criteria.orOperator(
                Criteria.where("code").regex(escaped, "i"),
                Criteria.where("name").regex(escaped, "i"),
            )
        }
        return mongoTemplate.find(
            Query(criteria).with(Sort.by(Sort.Direction.ASC, "code")),
            Warehouse::class.java,
        )
    }
}
