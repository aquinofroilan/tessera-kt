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
@Table(name = "inventory_uoms")
@EntityListeners(AuditingEntityListener::class)
data class UnitOfMeasure(
    @Id
    @Column(columnDefinition = "uuid")
    val id: UUID = UUID.randomUUID(),
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: UUID,
    val code: String,
    val name: String,
    val description: String? = null,
    @Column(name = "base_uom_id", columnDefinition = "uuid")
    val baseUomId: UUID? = null,
    @Column(name = "conversion_factor")
    val conversionFactor: BigDecimal = BigDecimal.ONE,
    @Column(name = "is_active")
    val isActive: Boolean = true,
    @CreatedDate
    @Column(name = "created_at")
    val createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,
)
