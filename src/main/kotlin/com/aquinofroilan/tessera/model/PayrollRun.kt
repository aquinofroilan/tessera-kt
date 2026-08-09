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

enum class PayrollRunStatus {
    DRAFT,
    APPROVED,
    PAID,
    CANCELLED,
}

@Entity
@Table(name = "payroll_run_lines")
class PayrollRunLine(
    @Id
    @Column(columnDefinition = "uuid")
    var id: java.util.UUID = java.util.UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "line_number")
    var lineNumber: Int = 0,
    @Column(name = "employee_id", columnDefinition = "uuid")
    var employeeId: java.util.UUID,
    @Column(name = "employee_number")
    var employeeNumber: String,
    @Column(name = "employee_name")
    var employeeName: String,
    @Column(name = "compensation_id", columnDefinition = "uuid")
    var compensationId: java.util.UUID,
    @Column(name = "gross_amount")
    var grossAmount: BigDecimal,
)

@Entity
@Table(name = "payroll_runs")
@EntityListeners(AuditingEntityListener::class)
class PayrollRun(
    @Id
    @Column(columnDefinition = "uuid")
    var id: java.util.UUID = java.util.UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "run_number")
    var runNumber: String,
    @Column(name = "period_start")
    var periodStart: LocalDate,
    @Column(name = "period_end")
    var periodEnd: LocalDate,
    @Column(name = "pay_date")
    var payDate: LocalDate,
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: java.util.UUID,
    @Enumerated(EnumType.STRING)
    var status: PayrollRunStatus = PayrollRunStatus.DRAFT,
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "payroll_run_id")
    @OrderBy("lineNumber ASC")
    var lines: List<PayrollRunLine>,
    @Column(name = "total_gross")
    var totalGross: BigDecimal,
    var currency: String,
    @Column(name = "created_by", columnDefinition = "uuid")
    var createdBy: java.util.UUID,
    @Column(name = "accrual_journal_entry_id", columnDefinition = "uuid")
    var accrualJournalEntryId: java.util.UUID? = null,
    @Column(name = "payment_journal_entry_id", columnDefinition = "uuid")
    var paymentJournalEntryId: java.util.UUID? = null,
    @Column(name = "approved_at")
    var approvedAt: LocalDateTime? = null,
    @Column(name = "approved_by", columnDefinition = "uuid")
    var approvedBy: java.util.UUID? = null,
    @Column(name = "paid_at")
    var paidAt: LocalDateTime? = null,
    @Column(name = "cancelled_at")
    var cancelledAt: LocalDateTime? = null,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)
