package com.aquinofroilan.tessera.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

enum class TimeEntryStatus {
    DRAFT,
    SUBMITTED,
    APPROVED,
    REJECTED,
}

@Entity
@Table(name = "time_entries")
@EntityListeners(AuditingEntityListener::class)
class TimeEntry(
    @Id
    @Column(columnDefinition = "uuid")
    var id: String = UUID.randomUUID().toString(),
    @Column(name = "employee_id", columnDefinition = "uuid")
    var employeeId: String,
    @Column(name = "project_id", columnDefinition = "uuid")
    var projectId: String,
    @Column(name = "task_id", columnDefinition = "uuid")
    var taskId: String? = null,
    @Column(name = "entry_date")
    var entryDate: LocalDate,
    var hours: BigDecimal,
    var billable: Boolean = true,
    var rate: BigDecimal? = null,
    @Enumerated(EnumType.STRING)
    var status: TimeEntryStatus = TimeEntryStatus.DRAFT,
    var notes: String? = null,
    @Column(name = "approved_by", columnDefinition = "uuid")
    var approvedBy: String? = null,
    @Column(name = "approved_at")
    var approvedAt: LocalDateTime? = null,
    var invoiced: Boolean = false,
    @Column(name = "invoice_id", columnDefinition = "uuid")
    var invoiceId: String? = null,
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: String,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)
