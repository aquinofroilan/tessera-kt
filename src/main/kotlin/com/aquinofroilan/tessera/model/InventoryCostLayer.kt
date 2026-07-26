package com.aquinofroilan.tessera.model

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
class InventoryCostLayer(
    @Id
    @Column(columnDefinition = "uuid")
    var id: java.util.UUID = UUID.randomUUID(),
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: java.util.UUID,
    @Column(name = "product_id", columnDefinition = "uuid")
    var productId: java.util.UUID,
    @Column(name = "warehouse_id", columnDefinition = "uuid")
    var warehouseId: java.util.UUID,
    @Column(name = "original_quantity")
    var originalQuantity: BigDecimal,
    @Column(name = "remaining_quantity")
    var remainingQuantity: BigDecimal,
    @Column(name = "unit_cost")
    var unitCost: BigDecimal,
    @Column(name = "source_movement_id", columnDefinition = "uuid")
    var sourceMovementId: java.util.UUID,
    @Column(name = "occurred_at")
    var occurredAt: LocalDateTime,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
)
