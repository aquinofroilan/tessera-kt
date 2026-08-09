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

enum class PaymentRunStatus {
    DRAFT,
    APPROVED,
    EXECUTED,
    CANCELLED,
}

enum class PaymentRunLineStatus {
    PENDING,
    PAID,
    SKIPPED,
    FAILED,
}

@Entity
@Table(name = "finance_payment_run_lines")
data class PaymentRunLine(
    @Id
    @Column(columnDefinition = "uuid")
    val id: java.util.UUID = java.util.UUID.randomUUID(),
    @Column(name = "line_number")
    val lineNumber: Int,
    @Column(name = "bill_id", columnDefinition = "uuid")
    val billId: java.util.UUID,
    @Column(name = "vendor_id", columnDefinition = "uuid")
    val vendorId: java.util.UUID,
    @Column(name = "vendor_name")
    val vendorName: String,
    @Column(name = "bill_number")
    val billNumber: String,
    val amount: BigDecimal,
    @Enumerated(EnumType.STRING)
    val status: PaymentRunLineStatus = PaymentRunLineStatus.PENDING,
    @Column(name = "bill_payment_id", columnDefinition = "uuid")
    val billPaymentId: java.util.UUID? = null,
    val notes: String? = null,
)

@Entity
@Table(name = "finance_payment_runs")
@EntityListeners(AuditingEntityListener::class)
data class PaymentRun(
    @Id
    @Column(columnDefinition = "uuid")
    val id: java.util.UUID = java.util.UUID.randomUUID(),
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: java.util.UUID,
    val code: String,
    @Column(name = "bank_account_id", columnDefinition = "uuid")
    val bankAccountId: java.util.UUID,
    @Column(name = "run_date")
    val runDate: LocalDate,
    @Enumerated(EnumType.STRING)
    val status: PaymentRunStatus = PaymentRunStatus.DRAFT,
    @Column(name = "total_amount")
    val totalAmount: BigDecimal = BigDecimal.ZERO,
    @Column(columnDefinition = "char(3)")
    val currency: String,
    val notes: String? = null,
    @Column(name = "created_by", columnDefinition = "uuid")
    val createdBy: java.util.UUID,
    @Column(name = "approved_at")
    val approvedAt: LocalDateTime? = null,
    @Column(name = "approved_by", columnDefinition = "uuid")
    val approvedBy: java.util.UUID? = null,
    @Column(name = "executed_at")
    val executedAt: LocalDateTime? = null,
    @Column(name = "executed_by", columnDefinition = "uuid")
    val executedBy: java.util.UUID? = null,
    @Column(name = "cancelled_at")
    val cancelledAt: LocalDateTime? = null,
    @Column(name = "cancelled_by", columnDefinition = "uuid")
    val cancelledBy: java.util.UUID? = null,
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "payment_run_id")
    @OrderBy("lineNumber ASC")
    val lines: List<PaymentRunLine>,
    @CreatedDate
    @Column(name = "created_at")
    val createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,
)
