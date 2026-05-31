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
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

enum class LeaveRequestStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED,
}

@Entity
@Table(name = "leave_requests")
@EntityListeners(AuditingEntityListener::class)
data class LeaveRequest(
    @Id
    @Column(columnDefinition = "uuid")
    val id: String = UUID.randomUUID().toString(),
    @Column(name = "employee_id", columnDefinition = "uuid")
    val employeeId: String,
    @Column(name = "leave_type_id", columnDefinition = "uuid")
    val leaveTypeId: String,
    @Column(name = "start_date")
    val startDate: LocalDate,
    @Column(name = "end_date")
    val endDate: LocalDate,
    val days: Int,
    val reason: String? = null,
    @Enumerated(EnumType.STRING)
    val status: LeaveRequestStatus = LeaveRequestStatus.PENDING,
    @Column(name = "decision_reason")
    val decisionReason: String? = null,
    @Column(name = "decided_by", columnDefinition = "uuid")
    val decidedBy: String? = null,
    @Column(name = "decided_at")
    val decidedAt: LocalDateTime? = null,
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: String,
    @Column(name = "requested_by", columnDefinition = "uuid")
    val requestedBy: String,
    @CreatedDate
    @Column(name = "created_at")
    val createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,
)
