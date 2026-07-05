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

enum class SalesOrderStatus {
    DRAFT,
    APPROVED,
    PARTIALLY_FULFILLED,
    FULFILLED,
    CLOSED,
    CANCELLED,
}

@Entity
@Table(name = "sales_order_lines")
class SalesOrderLine(
    @Id
    @Column(columnDefinition = "uuid")
    var id: String = UUID.randomUUID().toString(),
    @Column(name = "line_number")
    var lineNumber: Int = 0,
    @Column(name = "product_id", columnDefinition = "uuid")
    var productId: String,
    @Column(name = "product_sku")
    var productSku: String,
    @Column(name = "product_name")
    var productName: String,
    var quantity: BigDecimal,
    @Column(name = "unit_price")
    var unitPrice: BigDecimal,
    @Column(name = "line_total")
    var lineTotal: BigDecimal,
    @Column(name = "fulfilled_quantity")
    var fulfilledQuantity: BigDecimal = BigDecimal.ZERO,
    @Column(name = "invoiced_quantity")
    var invoicedQuantity: BigDecimal = BigDecimal.ZERO,
    var description: String? = null,
)

@Entity
@Table(name = "sales_orders")
@EntityListeners(AuditingEntityListener::class)
class SalesOrder(
    @Id
    @Column(columnDefinition = "uuid")
    var id: String = UUID.randomUUID().toString(),
    @Column(name = "so_number")
    var soNumber: String,
    @Column(name = "customer_id", columnDefinition = "uuid")
    var customerId: String,
    @Column(name = "customer_name")
    var customerName: String,
    @Column(name = "warehouse_id", columnDefinition = "uuid")
    var warehouseId: String,
    @Column(name = "order_date")
    var orderDate: LocalDate,
    @Column(name = "expected_date")
    var expectedDate: LocalDate? = null,
    @Column(name = "reference_number")
    var referenceNumber: String? = null,
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: String,
    @Enumerated(EnumType.STRING)
    var status: SalesOrderStatus = SalesOrderStatus.DRAFT,
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "sales_order_id")
    @OrderBy("lineNumber ASC")
    var lines: List<SalesOrderLine>,
    @Column(name = "total_amount")
    var totalAmount: BigDecimal,
    @Column(name = "created_by", columnDefinition = "uuid")
    var createdBy: String,
    @Column(name = "approved_at")
    var approvedAt: LocalDateTime? = null,
    @Column(name = "approved_by", columnDefinition = "uuid")
    var approvedBy: String? = null,
    @Column(name = "fulfilled_at")
    var fulfilledAt: LocalDateTime? = null,
    @Column(name = "cancelled_at")
    var cancelledAt: LocalDateTime? = null,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)
