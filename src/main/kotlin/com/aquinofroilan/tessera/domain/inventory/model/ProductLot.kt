package com.aquinofroilan.tessera.domain.inventory.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "product_lots")
@EntityListeners(AuditingEntityListener::class)
class ProductLot(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: UUID,
    @Column(name = "product_id", columnDefinition = "uuid")
    var productId: UUID,
    @Column(name = "lot_number")
    var lotNumber: String,
    @Column(name = "expiry_date")
    var expiryDate: LocalDate? = null,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)
