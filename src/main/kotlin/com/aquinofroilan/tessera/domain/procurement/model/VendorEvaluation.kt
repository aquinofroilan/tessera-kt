package com.aquinofroilan.tessera.domain.procurement.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@Entity
@Table(name = "vendor_evaluations")
@EntityListeners(AuditingEntityListener::class)
class VendorEvaluation(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "vendor_id", nullable = false, columnDefinition = "uuid")
    var vendorId: UUID,
    @Column(name = "organization_id", nullable = false, columnDefinition = "uuid")
    var organizationId: UUID,
    @Column(name = "purchase_order_id", columnDefinition = "uuid")
    var purchaseOrderId: UUID? = null,
    @Column(name = "evaluation_date", nullable = false)
    var evaluationDate: LocalDate = LocalDate.now(),
    @Column(name = "delivery_score", nullable = false, precision = 5, scale = 2)
    var deliveryScore: BigDecimal,
    @Column(name = "quality_score", nullable = false, precision = 5, scale = 2)
    var qualityScore: BigDecimal,
    @Column(name = "price_accuracy_score", nullable = false, precision = 5, scale = 2)
    var priceAccuracyScore: BigDecimal,
    @Column(name = "overall_score", nullable = false, precision = 5, scale = 2)
    var overallScore: BigDecimal,
    var comments: String? = null,
    @Column(name = "evaluated_by", nullable = false, columnDefinition = "uuid")
    var evaluatedBy: UUID,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
)
