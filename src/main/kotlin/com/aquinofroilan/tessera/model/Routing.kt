package com.aquinofroilan.tessera.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

enum class RoutingStatus {
    DRAFT,
    ACTIVE,
    OBSOLETE,
}

@Entity
@Table(name = "mfg_routing_operations")
data class RoutingOperation(
    @Id
    @Column(columnDefinition = "uuid")
    val id: java.util.UUID =
        java.util.UUID
            .ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "operation_number")
    val operationNumber: Int,
    @Column(name = "work_center_id", columnDefinition = "uuid")
    val workCenterId: java.util.UUID,
    @Column(name = "work_center_code")
    val workCenterCode: String,
    val description: String,
    @Column(name = "setup_minutes")
    val setupMinutes: BigDecimal = BigDecimal.ZERO,
    @Column(name = "run_minutes_per_unit")
    val runMinutesPerUnit: BigDecimal = BigDecimal.ZERO,
    @Column(name = "queue_minutes")
    val queueMinutes: BigDecimal = BigDecimal.ZERO,
    val instructions: String? = null,
)

@Entity
@Table(name = "mfg_routings")
@EntityListeners(AuditingEntityListener::class)
data class Routing(
    @Id
    @Column(columnDefinition = "uuid")
    val id: java.util.UUID =
        java.util.UUID
            .ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: java.util.UUID,
    @Column(name = "product_id", columnDefinition = "uuid")
    val productId: java.util.UUID,
    val code: String,
    val name: String,
    val version: Int = 1,
    @Enumerated(EnumType.STRING)
    val status: RoutingStatus = RoutingStatus.DRAFT,
    @Column(name = "is_default")
    val isDefault: Boolean = false,
    @Column(name = "effective_from")
    val effectiveFrom: LocalDate? = null,
    @Column(name = "effective_to")
    val effectiveTo: LocalDate? = null,
    val notes: String? = null,
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "routing_id")
    @OrderBy("operationNumber ASC")
    val operations: List<RoutingOperation>,
    @Column(name = "created_by", columnDefinition = "uuid")
    val createdBy: String,
    @Column(name = "activated_at")
    val activatedAt: LocalDateTime? = null,
    @Column(name = "activated_by", columnDefinition = "uuid")
    val activatedBy: String? = null,
    @Column(name = "obsoleted_at")
    val obsoletedAt: LocalDateTime? = null,
    @Column(name = "obsoleted_by", columnDefinition = "uuid")
    val obsoletedBy: String? = null,
    @CreatedDate
    @Column(name = "created_at")
    val createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,
)
