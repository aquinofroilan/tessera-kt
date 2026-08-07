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
class LeaveRequest(
    @Id
    @Column(columnDefinition = "uuid")
    var id: java.util.UUID = java.util.UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "employee_id", columnDefinition = "uuid")
    var employeeId: java.util.UUID,
    @Column(name = "leave_type_id", columnDefinition = "uuid")
    var leaveTypeId: java.util.UUID,
    @Column(name = "start_date")
    var startDate: LocalDate,
    @Column(name = "end_date")
    var endDate: LocalDate,
    var days: Int,
    var reason: String? = null,
    @Enumerated(EnumType.STRING)
    var status: LeaveRequestStatus = LeaveRequestStatus.PENDING,
    @Column(name = "decision_reason")
    var decisionReason: String? = null,
    @Column(name = "decided_by", columnDefinition = "uuid")
    var decidedBy: java.util.UUID? = null,
    @Column(name = "decided_at")
    var decidedAt: LocalDateTime? = null,
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: java.util.UUID,
    @Column(name = "requested_by", columnDefinition = "uuid")
    var requestedBy: java.util.UUID,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)
