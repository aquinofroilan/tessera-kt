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
    RECEIVED,
    CLOSED,
    CANCELLED,
}

@Entity
@Table(name = "purchase_order_lines")
data class PurchaseOrderLine(
    @Id
    @Column(columnDefinition = "uuid")
    val id: String = UUID.randomUUID().toString(),
    @Column(name = "line_number")
    val lineNumber: Int = 0,
    @Column(name = "product_id", columnDefinition = "uuid")
    val productId: String,
    @Column(name = "product_sku")
    val productSku: String,
    @Column(name = "product_name")
    val productName: String,
    val quantity: BigDecimal,
    @Column(name = "unit_cost")
    val unitCost: BigDecimal,
    @Column(name = "line_total")
    val lineTotal: BigDecimal,
    val description: String? = null,
)

@Entity
@Table(name = "purchase_orders")
@EntityListeners(AuditingEntityListener::class)
data class PurchaseOrder(
    @Id
    @Column(columnDefinition = "uuid")
    val id: String = UUID.randomUUID().toString(),
    @Column(name = "po_number")
    val poNumber: String,
    @Column(name = "vendor_id", columnDefinition = "uuid")
    val vendorId: String,
    @Column(name = "vendor_name")
    val vendorName: String,
    @Column(name = "warehouse_id", columnDefinition = "uuid")
    val warehouseId: String,
    @Column(name = "order_date")
    val orderDate: LocalDate,
    @Column(name = "expected_date")
    val expectedDate: LocalDate? = null,
    @Column(name = "reference_number")
    val referenceNumber: String? = null,
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: String,
    @Enumerated(EnumType.STRING)
    val status: PurchaseOrderStatus = PurchaseOrderStatus.DRAFT,
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "purchase_order_id")
    @OrderBy("lineNumber ASC")
    val lines: List<PurchaseOrderLine>,
    @Column(name = "total_amount")
    val totalAmount: BigDecimal,
    @Column(name = "created_by", columnDefinition = "uuid")
    val createdBy: String,
    @Column(name = "approved_at")
    val approvedAt: LocalDateTime? = null,
    @Column(name = "approved_by", columnDefinition = "uuid")
    val approvedBy: String? = null,
    @Column(name = "received_at")
    val receivedAt: LocalDateTime? = null,
    @Column(name = "cancelled_at")
    val cancelledAt: LocalDateTime? = null,
    @CreatedDate
    @Column(name = "created_at")
    val createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,
)
