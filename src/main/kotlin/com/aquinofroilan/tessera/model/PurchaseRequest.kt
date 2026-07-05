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
class PurchaseRequestLine(
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
    @Column(name = "estimated_unit_cost")
    var estimatedUnitCost: BigDecimal? = null,
    var description: String? = null,
)

@Entity
@Table(name = "purchase_requests")
@EntityListeners(AuditingEntityListener::class)
class PurchaseRequest(
    @Id
    @Column(columnDefinition = "uuid")
    var id: String = UUID.randomUUID().toString(),
    @Column(name = "pr_number")
    var prNumber: String,
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: String,
    @Enumerated(EnumType.STRING)
    var status: PurchaseRequestStatus = PurchaseRequestStatus.DRAFT,
    @Column(name = "suggested_vendor_id", columnDefinition = "uuid")
    var suggestedVendorId: String? = null,
    @Column(name = "warehouse_id", columnDefinition = "uuid")
    var warehouseId: String? = null,
    var justification: String? = null,
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "purchase_request_id")
    @OrderBy("lineNumber ASC")
    var lines: List<PurchaseRequestLine>,
    @Column(name = "requested_by", columnDefinition = "uuid")
    var requestedBy: String,
    @Column(name = "decided_by", columnDefinition = "uuid")
    var decidedBy: String? = null,
    @Column(name = "decided_at")
    var decidedAt: LocalDateTime? = null,
    @Column(name = "decision_reason")
    var decisionReason: String? = null,
    @Column(name = "converted_purchase_order_id", columnDefinition = "uuid")
    var convertedPurchaseOrderId: String? = null,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)
