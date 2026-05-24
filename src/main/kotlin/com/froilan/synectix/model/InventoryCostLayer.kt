package com.froilan.synectix.model

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Document(collection = "inventory_cost_layers")
@CompoundIndexes(
    CompoundIndex(
        name = "layers_org_product_warehouse_occurred",
        def = "{'organizationId': 1, 'productId': 1, 'warehouseId': 1, 'occurredAt': 1}",
    ),
)
data class InventoryCostLayer(
    @Id
    val id: String = UUID.randomUUID().toString(),
    val organizationId: String,
    val productId: String,
    val warehouseId: String,
    val originalQuantity: BigDecimal,
    val remainingQuantity: BigDecimal,
    val unitCost: BigDecimal,
    val sourceMovementId: String,
    val occurredAt: LocalDateTime,
    @CreatedDate
    var createdAt: LocalDateTime? = null,
)
