package com.aquinofroilan.tessera.domain.mfg.model

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
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

enum class SubcontractOrderStatus {
    DRAFT,
    DISPATCHED,
    PARTIALLY_RECEIVED,
    COMPLETED,
    CANCELLED,
}

@Entity
@Table(name = "mfg_subcontract_components")
class SubcontractComponent(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "product_id", nullable = false, columnDefinition = "uuid")
    var productId: UUID,
    @Column(name = "product_sku", nullable = false)
    var productSku: String,
    @Column(name = "product_name", nullable = false)
    var productName: String,
    @Column(name = "planned_quantity", nullable = false)
    var plannedQuantity: BigDecimal,
    @Column(name = "dispatched_quantity", nullable = false)
    var dispatchedQuantity: BigDecimal = BigDecimal.ZERO,
    var uom: String? = null,
)

@Entity
@Table(name = "mfg_subcontract_orders")
@EntityListeners(AuditingEntityListener::class)
class SubcontractOrder(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "organization_id", nullable = false, columnDefinition = "uuid")
    var organizationId: UUID,
    @Column(name = "order_number", nullable = false)
    var orderNumber: String,
    @Column(name = "work_order_id", nullable = false, columnDefinition = "uuid")
    var workOrderId: UUID,
    @Column(name = "operation_id", columnDefinition = "uuid")
    var operationId: UUID? = null,
    @Column(name = "operation_number", nullable = false)
    var operationNumber: Int,
    @Column(name = "vendor_id", nullable = false, columnDefinition = "uuid")
    var vendorId: UUID,
    @Column(name = "purchase_order_id", columnDefinition = "uuid")
    var purchaseOrderId: UUID? = null,
    @Column(name = "service_item_name", nullable = false)
    var serviceItemName: String,
    @Column(nullable = false)
    var quantity: BigDecimal,
    @Column(name = "received_quantity", nullable = false)
    var receivedQuantity: BigDecimal = BigDecimal.ZERO,
    @Column(name = "unit_service_cost", nullable = false)
    var unitServiceCost: BigDecimal = BigDecimal.ZERO,
    @Column(name = "total_cost", nullable = false)
    var totalCost: BigDecimal = BigDecimal.ZERO,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: SubcontractOrderStatus = SubcontractOrderStatus.DRAFT,
    @Column(name = "dispatched_at")
    var dispatchedAt: LocalDateTime? = null,
    @Column(name = "received_at")
    var receivedAt: LocalDateTime? = null,
    @Column(name = "completed_at")
    var completedAt: LocalDateTime? = null,
    @Column(name = "cancelled_at")
    var cancelledAt: LocalDateTime? = null,
    var notes: String? = null,
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "subcontract_order_id")
    var components: MutableList<SubcontractComponent> = mutableListOf(),
    @Column(name = "created_by", nullable = false, columnDefinition = "uuid")
    var createdBy: UUID,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)
