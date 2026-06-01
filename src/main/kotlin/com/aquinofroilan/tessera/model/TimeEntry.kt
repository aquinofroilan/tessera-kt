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
data class TimeEntry(
    @Id
    @Column(columnDefinition = "uuid")
    val id: String = UUID.randomUUID().toString(),
    @Column(name = "employee_id", columnDefinition = "uuid")
    val employeeId: String,
    @Column(name = "project_id", columnDefinition = "uuid")
    val projectId: String,
    @Column(name = "task_id", columnDefinition = "uuid")
    val taskId: String? = null,
    @Column(name = "entry_date")
    val entryDate: LocalDate,
    val hours: BigDecimal,
    val billable: Boolean = true,
    val rate: BigDecimal? = null,
    @Enumerated(EnumType.STRING)
    val status: TimeEntryStatus = TimeEntryStatus.DRAFT,
    val notes: String? = null,
    @Column(name = "approved_by", columnDefinition = "uuid")
    val approvedBy: String? = null,
    @Column(name = "approved_at")
    val approvedAt: LocalDateTime? = null,
    val invoiced: Boolean = false,
    @Column(name = "invoice_id", columnDefinition = "uuid")
    val invoiceId: String? = null,
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: String,
    @CreatedDate
    @Column(name = "created_at")
    val createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,
)
