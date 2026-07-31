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
class InventoryWaSnapshot(
    @Id
    @Column(columnDefinition = "uuid")
    var id: java.util.UUID = java.util.UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: java.util.UUID,
    @Column(name = "product_id", columnDefinition = "uuid")
    var productId: java.util.UUID,
    @Column(name = "warehouse_id", columnDefinition = "uuid")
    var warehouseId: java.util.UUID,
    var quantity: BigDecimal,
    @Column(name = "total_cost")
    var totalCost: BigDecimal,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)
