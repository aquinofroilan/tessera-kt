package com.froilan.synectix.model

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Document(collection = "products")
@CompoundIndexes(
    CompoundIndex(
        name = "unique_sku_per_org",
        def = "{'organizationId': 1, 'sku': 1}",
        unique = true,
    ),
    CompoundIndex(
        name = "products_org_active",
        def = "{'organizationId': 1, 'isActive': 1}",
    ),
    CompoundIndex(
        name = "products_org_category",
        def = "{'organizationId': 1, 'category': 1}",
    ),
)
data class Product(
    @Id
    val id: String = UUID.randomUUID().toString(),
    val sku: String,
    val name: String,
    val description: String? = null,
    val category: String? = null,
    val imageUrl: String? = null,
    val listPrice: BigDecimal,
    val priceCurrency: String,
    val taxGroupId: String? = null,
    @Indexed
    val organizationId: String,
    val isActive: Boolean = true,
    @CreatedDate
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    var updatedAt: LocalDateTime? = null,
)
