package com.froilan.synectix.model

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

enum class StockMovementType {
    RECEIPT,
    ISSUE,
    TRANSFER,
    ADJUSTMENT,
    OPENING_BALANCE,
}

@Document(collection = "stock_movements")
@CompoundIndexes(
    CompoundIndex(
        name = "movements_org_product_warehouse",
        def = "{'organizationId': 1, 'productId': 1, 'warehouseId': 1}",
    ),
    CompoundIndex(
        name = "movements_org_transferTo",
        def = "{'organizationId': 1, 'transferToWarehouseId': 1}",
    ),
    CompoundIndex(
        name = "movements_org_occurred",
        def = "{'organizationId': 1, 'occurredAt': -1}",
    ),
)
data class StockMovement(
    @Id
    val id: String = UUID.randomUUID().toString(),
    val organizationId: String,
    val type: StockMovementType,
    val productId: String,
    val warehouseId: String,
    val transferToWarehouseId: String? = null,
    val quantity: BigDecimal,
    val unitCost: BigDecimal? = null,
    val reference: String? = null,
    val notes: String? = null,
    val occurredAt: LocalDateTime,
    val createdBy: String,
    @CreatedDate
    var createdAt: LocalDateTime? = null,
)
