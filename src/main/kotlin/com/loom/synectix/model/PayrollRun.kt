package com.loom.synectix.model

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

enum class PayrollRunStatus {
    DRAFT,
    APPROVED,
    PAID,
    CANCELLED,
}

@Entity
@Table(name = "payroll_run_lines")
data class PayrollRunLine(
    @Id
    @Column(columnDefinition = "uuid")
    val id: String = UUID.randomUUID().toString(),
    @Column(name = "line_number")
    val lineNumber: Int = 0,
    @Column(name = "employee_id", columnDefinition = "uuid")
    val employeeId: String,
    @Column(name = "employee_number")
    val employeeNumber: String,
    @Column(name = "employee_name")
    val employeeName: String,
    @Column(name = "compensation_id", columnDefinition = "uuid")
    val compensationId: String,
    @Column(name = "gross_amount")
    val grossAmount: BigDecimal,
)

@Entity
@Table(name = "payroll_runs")
@EntityListeners(AuditingEntityListener::class)
data class PayrollRun(
    @Id
    @Column(columnDefinition = "uuid")
    val id: String = UUID.randomUUID().toString(),
    @Column(name = "run_number")
    val runNumber: String,
    @Column(name = "period_start")
    val periodStart: LocalDate,
    @Column(name = "period_end")
    val periodEnd: LocalDate,
    @Column(name = "pay_date")
    val payDate: LocalDate,
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: String,
    @Enumerated(EnumType.STRING)
    val status: PayrollRunStatus = PayrollRunStatus.DRAFT,
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "payroll_run_id")
    @OrderBy("lineNumber ASC")
    val lines: List<PayrollRunLine>,
    @Column(name = "total_gross")
    val totalGross: BigDecimal,
    val currency: String,
    @Column(name = "created_by", columnDefinition = "uuid")
    val createdBy: String,
    @Column(name = "accrual_journal_entry_id", columnDefinition = "uuid")
    val accrualJournalEntryId: String? = null,
    @Column(name = "payment_journal_entry_id", columnDefinition = "uuid")
    val paymentJournalEntryId: String? = null,
    @Column(name = "approved_at")
    val approvedAt: LocalDateTime? = null,
    @Column(name = "approved_by", columnDefinition = "uuid")
    val approvedBy: String? = null,
    @Column(name = "paid_at")
    val paidAt: LocalDateTime? = null,
    @Column(name = "cancelled_at")
    val cancelledAt: LocalDateTime? = null,
    @CreatedDate
    @Column(name = "created_at")
    val createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,
)
