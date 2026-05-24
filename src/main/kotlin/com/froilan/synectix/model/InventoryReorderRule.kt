package com.froilan.synectix.model

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Document(collection = "inventory_reorder_rules")
@CompoundIndex(
    name = "reorder_unique_org_product_warehouse",
    def = "{'organizationId': 1, 'productId': 1, 'warehouseId': 1}",
    unique = true,
)
data class InventoryReorderRule(
    @Id
    val id: String = UUID.randomUUID().toString(),
    val organizationId: String,
    val productId: String,
    val warehouseId: String,
    val reorderPoint: BigDecimal,
    val safetyStock: BigDecimal = BigDecimal.ZERO,
    @CreatedDate
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    var updatedAt: LocalDateTime? = null,
)
