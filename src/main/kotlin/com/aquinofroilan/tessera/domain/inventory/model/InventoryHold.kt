package com.aquinofroilan.tessera.domain.inventory.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

enum class HoldStatus {
    ACTIVE,
    RELEASED,
}

@Entity
@Table(name = "inventory_holds")
@EntityListeners(AuditingEntityListener::class)
class InventoryHold(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: UUID,
    @Column(name = "product_id", columnDefinition = "uuid")
    var productId: UUID,
    @Column(name = "warehouse_id", columnDefinition = "uuid")
    var warehouseId: UUID,
    @Column(name = "lot_number")
    var lotNumber: String? = null,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "serial_numbers", columnDefinition = "jsonb")
    var serialNumbers: List<String>? = null,
    var quantity: BigDecimal,
    var reference: String? = null,
    var notes: String? = null,
    @Enumerated(EnumType.STRING)
    var status: HoldStatus = HoldStatus.ACTIVE,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
    @CreatedBy
    @Column(name = "created_by", columnDefinition = "uuid")
    var createdBy: UUID? = null,
    @Column(name = "released_at")
    var releasedAt: LocalDateTime? = null,
    @Column(name = "released_by", columnDefinition = "uuid")
    var releasedBy: UUID? = null,
)
