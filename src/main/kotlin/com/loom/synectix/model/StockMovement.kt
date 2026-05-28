package com.loom.synectix.model

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
}

@Entity
@Table(name = "stock_movements")
@EntityListeners(AuditingEntityListener::class)
data class StockMovement(
    @Id
    @Column(columnDefinition = "uuid")
    val id: String = UUID.randomUUID().toString(),
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: String,
    @Enumerated(EnumType.STRING)
    val type: StockMovementType,
    @Column(name = "product_id", columnDefinition = "uuid")
    val productId: String,
    @Column(name = "warehouse_id", columnDefinition = "uuid")
    val warehouseId: String,
    @Column(name = "transfer_to_warehouse_id", columnDefinition = "uuid")
    val transferToWarehouseId: String? = null,
    val quantity: BigDecimal,
    @Column(name = "unit_cost")
    val unitCost: BigDecimal? = null,
    val reference: String? = null,
    val notes: String? = null,
    @Column(name = "occurred_at")
    val occurredAt: LocalDateTime,
    @Column(name = "created_by", columnDefinition = "uuid")
    val createdBy: String,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
)
