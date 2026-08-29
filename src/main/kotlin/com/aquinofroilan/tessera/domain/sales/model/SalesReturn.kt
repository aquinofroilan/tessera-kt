package com.aquinofroilan.tessera.domain.sales.model

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
import java.time.ZoneOffset
import java.util.UUID

enum class SalesReturnStatus {
    REQUESTED,
    APPROVED,
    RECEIVED,
    COMPLETED,
    CANCELLED,
}

enum class ReturnReason {
    DEFECTIVE,
    WRONG_ITEM,
    BUYER_REMORSE,
    DAMAGED_IN_TRANSIT,
    EXPIRED,
    OTHER,
}

@Entity
@Table(name = "sales_return_lines")
class SalesReturnLine(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "sales_return_id", nullable = false, columnDefinition = "uuid")
    var salesReturnId: UUID,
    @Column(name = "line_number", nullable = false)
    var lineNumber: Int = 0,
    @Column(name = "product_id", nullable = false, columnDefinition = "uuid")
    var productId: UUID,
    @Column(name = "product_sku", nullable = false)
    var productSku: String,
    @Column(name = "product_name", nullable = false)
    var productName: String,
    @Column(nullable = false, precision = 19, scale = 4)
    var quantity: BigDecimal,
    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    var unitPrice: BigDecimal,
    @Column(name = "line_total", nullable = false, precision = 19, scale = 4)
    var lineTotal: BigDecimal,
    @Column(name = "received_quantity", nullable = false, precision = 19, scale = 4)
    var receivedQuantity: BigDecimal = BigDecimal.ZERO,
    @Column(name = "condition_notes")
    var conditionNotes: String? = null,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
)

@Entity
@Table(name = "sales_returns")
@EntityListeners(AuditingEntityListener::class)
class SalesReturn(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "organization_id", nullable = false, columnDefinition = "uuid")
    var organizationId: UUID,
    @Column(name = "return_number", nullable = false)
    var returnNumber: String,
    @Column(name = "customer_id", nullable = false, columnDefinition = "uuid")
    var customerId: UUID,
    @Column(name = "customer_name", nullable = false)
    var customerName: String,
    @Column(name = "sales_order_id", columnDefinition = "uuid")
    var salesOrderId: UUID? = null,
    @Column(name = "invoice_id", columnDefinition = "uuid")
    var invoiceId: UUID? = null,
    @Column(name = "warehouse_id", nullable = false, columnDefinition = "uuid")
    var warehouseId: UUID,
    @Column(name = "return_date", nullable = false)
    var returnDate: LocalDate = LocalDate.now(),
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: SalesReturnStatus = SalesReturnStatus.REQUESTED,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var reason: ReturnReason,
    var notes: String? = null,
    @Column(name = "restock_inventory", nullable = false)
    var restockInventory: Boolean = true,
    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    var totalAmount: BigDecimal,
    @Column(name = "created_by", nullable = false, columnDefinition = "uuid")
    var createdBy: UUID,
    @Column(name = "approved_by", columnDefinition = "uuid")
    var approvedBy: UUID? = null,
    @Column(name = "approved_at")
    var approvedAt: LocalDateTime? = null,
    @Column(name = "received_by", columnDefinition = "uuid")
    var receivedBy: UUID? = null,
    @Column(name = "received_at")
    var receivedAt: LocalDateTime? = null,
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "sales_return_id")
    @OrderBy("lineNumber ASC")
    var lines: MutableList<SalesReturnLine> = mutableListOf(),
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
)
