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
@Table(name = "stock_on_hand")
@EntityListeners(AuditingEntityListener::class)
class StockOnHand(
    @Id
    @Column(columnDefinition = "uuid")
    var id: String = UUID.randomUUID().toString(),
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: String,
    @Column(name = "product_id", columnDefinition = "uuid")
    var productId: String,
    @Column(name = "warehouse_id", columnDefinition = "uuid")
    var warehouseId: String,
    var quantity: BigDecimal = BigDecimal.ZERO,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)
