package com.aquinofroilan.tessera.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

enum class StockMovementType {
    RECEIPT,
    ISSUE,
    TRANSFER,
    ADJUSTMENT,
    OPENING_BALANCE,
    WIP_ISSUE,
    WIP_RECEIPT,
}

@Entity
@Table(name = "stock_movements")
@EntityListeners(AuditingEntityListener::class)
class StockMovement(
    @Id
    @Column(columnDefinition = "uuid")
    var id: java.util.UUID = java.util.UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: java.util.UUID,
    @Enumerated(EnumType.STRING)
    var type: StockMovementType,
    @Column(name = "product_id", columnDefinition = "uuid")
    var productId: java.util.UUID,
    @Column(name = "warehouse_id", columnDefinition = "uuid")
    var warehouseId: java.util.UUID,
    @Column(name = "transfer_to_warehouse_id", columnDefinition = "uuid")
    var transferToWarehouseId: java.util.UUID? = null,
    var quantity: BigDecimal,
    @Column(name = "unit_cost")
    var unitCost: BigDecimal? = null,
    var reference: String? = null,
    var notes: String? = null,
    @Column(name = "reversed")
    var reversed: Boolean = false,
    @Column(name = "reversal_of_movement_id", columnDefinition = "uuid")
    var reversalOfMovementId: java.util.UUID? = null,
    @Column(name = "occurred_at")
    var occurredAt: LocalDateTime,
    @Column(name = "created_by", columnDefinition = "uuid")
    var createdBy: java.util.UUID,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
)
