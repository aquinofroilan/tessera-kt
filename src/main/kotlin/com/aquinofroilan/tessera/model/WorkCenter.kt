package com.aquinofroilan.tessera.model

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
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

enum class WorkCenterType {
    MACHINE,
    LABOR,
    CELL,
    ASSEMBLY,
    SUBCONTRACT,
}

@Entity
@Table(name = "mfg_work_centers")
@EntityListeners(AuditingEntityListener::class)
data class WorkCenter(
    @Id
    @Column(columnDefinition = "uuid")
    val id: String = UUID.randomUUID().toString(),
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: String,
    val code: String,
    val name: String,
    val description: String? = null,
    @Enumerated(EnumType.STRING)
    val type: WorkCenterType = WorkCenterType.MACHINE,
    @Column(name = "warehouse_id", columnDefinition = "uuid")
    val warehouseId: String? = null,
    @Column(name = "capacity_per_hour")
    val capacityPerHour: BigDecimal = BigDecimal.ONE,
    @Column(name = "cost_per_hour")
    val costPerHour: BigDecimal = BigDecimal.ZERO,
    @Column(name = "efficiency_pct")
    val efficiencyPct: BigDecimal = BigDecimal(100),
    @Column(name = "is_active")
    val isActive: Boolean = true,
    @CreatedDate
    @Column(name = "created_at")
    val createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,
)
