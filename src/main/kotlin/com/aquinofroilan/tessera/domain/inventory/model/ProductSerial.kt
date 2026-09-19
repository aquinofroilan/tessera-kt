package com.aquinofroilan.tessera.domain.inventory.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.util.UUID

enum class SerialStatus {
    IN_STOCK,
    ISSUED,
    ADJUSTED_OUT
}

@Entity
@Table(name = "product_serials")
@EntityListeners(AuditingEntityListener::class)
class ProductSerial(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: UUID,
    @Column(name = "product_id", columnDefinition = "uuid")
    var productId: UUID,
    @Column(name = "serial_number")
    var serialNumber: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    var status: SerialStatus,
    @Column(name = "current_warehouse_id", columnDefinition = "uuid")
    var currentWarehouseId: UUID? = null,
    @Column(name = "lot_number")
    var lotNumber: String? = null,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)
