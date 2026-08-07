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

enum class WorkOrderStatus {
    DRAFT,
    RELEASED,
    IN_PROGRESS,
    COMPLETED,
    CLOSED,
    CANCELLED,
}

enum class WorkOrderOperationStatus {
    PENDING,
    IN_PROGRESS,
    DONE,
    SKIPPED,
}

@Entity
@Table(name = "mfg_wo_components")
data class WorkOrderComponent(
    @Id
    @Column(columnDefinition = "uuid")
    val id: java.util.UUID =
        java.util.UUID
            .ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "line_number")
    val lineNumber: Int,
    @Column(name = "component_product_id", columnDefinition = "uuid")
    val componentProductId: java.util.UUID,
    @Column(name = "component_sku")
    val componentSku: String,
    @Column(name = "component_name")
    val componentName: String,
    @Column(name = "planned_quantity")
    val plannedQuantity: BigDecimal,
    @Column(name = "issued_quantity")
    val issuedQuantity: BigDecimal = BigDecimal.ZERO,
    @Column(name = "issued_cost")
    val issuedCost: BigDecimal = BigDecimal.ZERO,
    val uom: String? = null,
    @Column(name = "scrap_pct")
    val scrapPct: BigDecimal = BigDecimal.ZERO,
)

@Entity
@Table(name = "mfg_wo_operations")
data class WorkOrderOperation(
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
    @Column(name = "planned_setup_minutes")
    val plannedSetupMinutes: BigDecimal = BigDecimal.ZERO,
    @Column(name = "planned_run_minutes_per_unit")
    val plannedRunMinutesPerUnit: BigDecimal = BigDecimal.ZERO,
    @Column(name = "actual_minutes")
    val actualMinutes: BigDecimal = BigDecimal.ZERO,
    @Enumerated(EnumType.STRING)
    val status: WorkOrderOperationStatus = WorkOrderOperationStatus.PENDING,
)

@Entity
@Table(name = "mfg_work_orders")
@EntityListeners(AuditingEntityListener::class)
data class WorkOrder(
    @Id
    @Column(columnDefinition = "uuid")
    val id: java.util.UUID =
        java.util.UUID
            .ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: java.util.UUID,
    @Column(name = "wo_number")
    val woNumber: String,
    @Column(name = "product_id", columnDefinition = "uuid")
    val productId: java.util.UUID,
    @Column(name = "product_sku")
    val productSku: String,
    @Column(name = "product_name")
    val productName: String,
    @Column(name = "bom_id", columnDefinition = "uuid")
    val bomId: java.util.UUID,
    @Column(name = "routing_id", columnDefinition = "uuid")
    val routingId: java.util.UUID? = null,
    val quantity: BigDecimal,
    @Column(name = "quantity_completed")
    val quantityCompleted: BigDecimal = BigDecimal.ZERO,
    @Column(name = "quantity_scrapped")
    val quantityScrapped: BigDecimal = BigDecimal.ZERO,
    @Column(name = "total_issued_cost")
    val totalIssuedCost: BigDecimal = BigDecimal.ZERO,
    @Column(name = "total_completed_cost")
    val totalCompletedCost: BigDecimal = BigDecimal.ZERO,
    @Column(name = "source_warehouse_id", columnDefinition = "uuid")
    val sourceWarehouseId: java.util.UUID,
    @Column(name = "target_warehouse_id", columnDefinition = "uuid")
    val targetWarehouseId: java.util.UUID,
    @Enumerated(EnumType.STRING)
    val status: WorkOrderStatus = WorkOrderStatus.DRAFT,
    @Column(name = "planned_start")
    val plannedStart: LocalDate? = null,
    @Column(name = "planned_end")
    val plannedEnd: LocalDate? = null,
    @Column(name = "released_at")
    val releasedAt: LocalDateTime? = null,
    @Column(name = "released_by", columnDefinition = "uuid")
    val releasedBy: String? = null,
    @Column(name = "started_at")
    val startedAt: LocalDateTime? = null,
    @Column(name = "completed_at")
    val completedAt: LocalDateTime? = null,
    @Column(name = "completed_by", columnDefinition = "uuid")
    val completedBy: String? = null,
    @Column(name = "cancelled_at")
    val cancelledAt: LocalDateTime? = null,
    @Column(name = "cancelled_by", columnDefinition = "uuid")
    val cancelledBy: String? = null,
    val notes: String? = null,
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "work_order_id")
    @OrderBy("lineNumber ASC")
    val components: List<WorkOrderComponent>,
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "work_order_id")
    @OrderBy("operationNumber ASC")
    val operations: List<WorkOrderOperation>,
    @Column(name = "created_by", columnDefinition = "uuid")
    val createdBy: String,
    @CreatedDate
    @Column(name = "created_at")
    val createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,
)
