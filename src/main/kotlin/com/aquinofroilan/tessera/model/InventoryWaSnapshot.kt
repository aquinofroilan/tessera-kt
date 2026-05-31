package com.aquinofroilan.tessera.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "inventory_wa_snapshots")
@EntityListeners(AuditingEntityListener::class)
data class InventoryWaSnapshot(
    @Id
    @Column(columnDefinition = "uuid")
    val id: String = UUID.randomUUID().toString(),
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: String,
    @Column(name = "product_id", columnDefinition = "uuid")
    val productId: String,
    @Column(name = "warehouse_id", columnDefinition = "uuid")
    val warehouseId: String,
    val quantity: BigDecimal,
    @Column(name = "total_cost")
    val totalCost: BigDecimal,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)
