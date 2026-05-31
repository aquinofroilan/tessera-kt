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
import java.time.LocalDateTime
import java.util.UUID

enum class PurchaseRequestStatus {
    DRAFT,
    SUBMITTED,
    APPROVED,
    REJECTED,
    CONVERTED,
    CANCELLED,
}

@Entity
@Table(name = "purchase_request_lines")
data class PurchaseRequestLine(
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
    @Column(name = "estimated_unit_cost")
    val estimatedUnitCost: BigDecimal? = null,
    val description: String? = null,
)

@Entity
@Table(name = "purchase_requests")
@EntityListeners(AuditingEntityListener::class)
data class PurchaseRequest(
    @Id
    @Column(columnDefinition = "uuid")
    val id: String = UUID.randomUUID().toString(),
    @Column(name = "pr_number")
    val prNumber: String,
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: String,
    @Enumerated(EnumType.STRING)
    val status: PurchaseRequestStatus = PurchaseRequestStatus.DRAFT,
    @Column(name = "suggested_vendor_id", columnDefinition = "uuid")
    val suggestedVendorId: String? = null,
    @Column(name = "warehouse_id", columnDefinition = "uuid")
    val warehouseId: String? = null,
    val justification: String? = null,
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "purchase_request_id")
    @OrderBy("lineNumber ASC")
    val lines: List<PurchaseRequestLine>,
    @Column(name = "requested_by", columnDefinition = "uuid")
    val requestedBy: String,
    @Column(name = "decided_by", columnDefinition = "uuid")
    val decidedBy: String? = null,
    @Column(name = "decided_at")
    val decidedAt: LocalDateTime? = null,
    @Column(name = "decision_reason")
    val decisionReason: String? = null,
    @Column(name = "converted_purchase_order_id", columnDefinition = "uuid")
    val convertedPurchaseOrderId: String? = null,
    @CreatedDate
    @Column(name = "created_at")
    val createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,
)
