package com.froilan.synectix.repository

import com.froilan.synectix.model.Product
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query

interface ProductQueries {
    fun search(
        organizationId: String,
        isActive: Boolean,
        category: String?,
        term: String?,
    ): List<Product>
}

open class ProductQueriesImpl(
    private val mongoTemplate: MongoTemplate,
) : ProductQueries {
    override fun search(
        organizationId: String,
        isActive: Boolean,
        category: String?,
        term: String?,
    ): List<Product> {
        val criteria =
            Criteria
                .where("organizationId")
                .`is`(organizationId)
                .and("isActive")
                .`is`(isActive)
        if (category != null) {
            criteria.and("category").`is`(category)
        }
        val cleaned = term?.trim()?.takeIf { it.isNotEmpty() }
        if (cleaned != null) {
            val escaped = Regex.escape(cleaned)
            criteria.orOperator(
                Criteria.where("sku").regex(escaped, "i"),
                Criteria.where("name").regex(escaped, "i"),
            )
        }
        return mongoTemplate.find(
            Query(criteria).with(Sort.by(Sort.Direction.ASC, "sku")),
            Product::class.java,
        )
    }
}
