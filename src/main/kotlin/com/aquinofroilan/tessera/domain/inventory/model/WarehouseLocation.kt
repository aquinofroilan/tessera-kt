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

enum class LocationType {
    ZONE,
    AISLE,
    RACK,
    SHELF,
    BIN,
}

@Entity
@Table(name = "warehouse_locations")
@EntityListeners(AuditingEntityListener::class)
class WarehouseLocation(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID(),
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: UUID,
    @Column(name = "warehouse_id", columnDefinition = "uuid")
    var warehouseId: UUID,
    @Column(name = "parent_location_id", columnDefinition = "uuid")
    var parentLocationId: UUID? = null,
    var code: String,
    @Enumerated(EnumType.STRING)
    var type: LocationType,
    var barcode: String? = null,
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)
