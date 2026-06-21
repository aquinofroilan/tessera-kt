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

enum class QuotationStatus {
    DRAFT,
    SENT,
    ACCEPTED,
    REJECTED,
    CONVERTED,
    CANCELLED,
}

@Entity
@Table(name = "quotation_lines")
data class QuotationLine(
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
    @Column(name = "unit_price")
    val unitPrice: BigDecimal,
    @Column(name = "line_total")
    val lineTotal: BigDecimal,
    val description: String? = null,
)

@Entity
@Table(name = "quotations")
@EntityListeners(AuditingEntityListener::class)
data class Quotation(
    @Id
    @Column(columnDefinition = "uuid")
    val id: String = UUID.randomUUID().toString(),
    @Column(name = "quote_number")
    val quoteNumber: String,
    @Column(name = "customer_id", columnDefinition = "uuid")
    val customerId: String,
    @Column(name = "customer_name")
    val customerName: String,
    @Column(name = "warehouse_id", columnDefinition = "uuid")
    val warehouseId: String? = null,
    @Column(name = "quote_date")
    val quoteDate: LocalDate,
    @Column(name = "valid_until")
    val validUntil: LocalDate? = null,
    @Column(name = "reference_number")
    val referenceNumber: String? = null,
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: String,
    @Enumerated(EnumType.STRING)
    val status: QuotationStatus = QuotationStatus.DRAFT,
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "quotation_id")
    @OrderBy("lineNumber ASC")
    val lines: List<QuotationLine>,
    @Column(name = "total_amount")
    val totalAmount: BigDecimal,
    @Column(name = "created_by", columnDefinition = "uuid")
    val createdBy: String,
    @Column(name = "sent_at")
    val sentAt: LocalDateTime? = null,
    @Column(name = "decided_at")
    val decidedAt: LocalDateTime? = null,
    @Column(name = "decision_reason")
    val decisionReason: String? = null,
    @Column(name = "converted_sales_order_id", columnDefinition = "uuid")
    val convertedSalesOrderId: String? = null,
    @CreatedDate
    @Column(name = "created_at")
    val createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,
)
