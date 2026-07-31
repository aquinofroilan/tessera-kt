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

enum class PurchaseOrderStatus {
    DRAFT,
    APPROVED,
    PARTIALLY_RECEIVED,
    RECEIVED,
    CLOSED,
    CANCELLED,
}

@Entity
@Table(name = "purchase_order_lines")
class PurchaseOrderLine(
    @Id
    @Column(columnDefinition = "uuid")
    var id: java.util.UUID = java.util.UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "line_number")
    var lineNumber: Int = 0,
    @Column(name = "product_id", columnDefinition = "uuid")
    var productId: java.util.UUID,
    @Column(name = "product_sku")
    var productSku: String,
    @Column(name = "product_name")
    var productName: String,
    var quantity: BigDecimal,
    @Column(name = "unit_cost")
    var unitCost: BigDecimal,
    @Column(name = "line_total")
    var lineTotal: BigDecimal,
    @Column(name = "received_quantity")
    var receivedQuantity: BigDecimal = BigDecimal.ZERO,
    @Column(name = "billed_quantity")
    var billedQuantity: BigDecimal = BigDecimal.ZERO,
    var description: String? = null,
)

@Entity
@Table(name = "purchase_orders")
@EntityListeners(AuditingEntityListener::class)
class PurchaseOrder(
    @Id
    @Column(columnDefinition = "uuid")
    var id: java.util.UUID = java.util.UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "po_number")
    var poNumber: String,
    @Column(name = "vendor_id", columnDefinition = "uuid")
    var vendorId: java.util.UUID,
    @Column(name = "vendor_name")
    var vendorName: String,
    @Column(name = "warehouse_id", columnDefinition = "uuid")
    var warehouseId: java.util.UUID,
    @Column(name = "order_date")
    var orderDate: LocalDate,
    @Column(name = "expected_date")
    var expectedDate: LocalDate? = null,
    @Column(name = "reference_number")
    var referenceNumber: String? = null,
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: java.util.UUID,
    @Enumerated(EnumType.STRING)
    var status: PurchaseOrderStatus = PurchaseOrderStatus.DRAFT,
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "purchase_order_id")
    @OrderBy("lineNumber ASC")
    var lines: List<PurchaseOrderLine>,
    @Column(name = "total_amount")
    var totalAmount: BigDecimal,
    @Column(name = "created_by", columnDefinition = "uuid")
    var createdBy: java.util.UUID,
    @Column(name = "approved_at")
    var approvedAt: LocalDateTime? = null,
    @Column(name = "approved_by", columnDefinition = "uuid")
    var approvedBy: java.util.UUID? = null,
    @Column(name = "received_at")
    var receivedAt: LocalDateTime? = null,
    @Column(name = "cancelled_at")
    var cancelledAt: LocalDateTime? = null,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)
