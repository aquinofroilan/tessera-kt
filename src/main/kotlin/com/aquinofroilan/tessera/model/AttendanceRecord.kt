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

enum class AttendanceStatus {
    PRESENT,
    ABSENT,
    ON_LEAVE,
}

@Entity
@Table(name = "attendance_records")
@EntityListeners(AuditingEntityListener::class)
class AttendanceRecord(
    @Id
    @Column(columnDefinition = "uuid")
    var id: String = UUID.randomUUID().toString(),
    @Column(name = "employee_id", columnDefinition = "uuid")
    var employeeId: String,
    @Column(name = "work_date")
    var workDate: LocalDate,
    @Column(name = "clock_in")
    var clockIn: LocalDateTime? = null,
    @Column(name = "clock_out")
    var clockOut: LocalDateTime? = null,
    @Column(name = "worked_minutes")
    var workedMinutes: Int? = null,
    @Enumerated(EnumType.STRING)
    var status: AttendanceStatus = AttendanceStatus.PRESENT,
    var notes: String? = null,
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: String,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)
