package com.aquinofroilan.tessera.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "inventory_reorder_rules")
@EntityListeners(AuditingEntityListener::class)
data class InventoryReorderRule(
    @Id
    @Column(columnDefinition = "uuid")
    val id: String = UUID.randomUUID().toString(),
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: String,
    @Column(name = "product_id", columnDefinition = "uuid")
    val productId: String,
    @Column(name = "warehouse_id", columnDefinition = "uuid")
    val warehouseId: String,
    @Column(name = "reorder_point")
    val reorderPoint: BigDecimal,
    @Column(name = "safety_stock")
    val safetyStock: BigDecimal = BigDecimal.ZERO,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)
