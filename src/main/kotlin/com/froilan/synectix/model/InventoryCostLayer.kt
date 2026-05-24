package com.froilan.synectix.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "inventory_cost_layers")
@EntityListeners(AuditingEntityListener::class)
data class InventoryCostLayer(
    @Id
    @Column(columnDefinition = "uuid")
    val id: String = UUID.randomUUID().toString(),
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: String,
    @Column(name = "product_id", columnDefinition = "uuid")
    val productId: String,
    @Column(name = "warehouse_id", columnDefinition = "uuid")
    val warehouseId: String,
    @Column(name = "original_quantity")
    val originalQuantity: BigDecimal,
    @Column(name = "remaining_quantity")
    val remainingQuantity: BigDecimal,
    @Column(name = "unit_cost")
    val unitCost: BigDecimal,
    @Column(name = "source_movement_id", columnDefinition = "uuid")
    val sourceMovementId: String,
    @Column(name = "occurred_at")
    val occurredAt: LocalDateTime,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
)
