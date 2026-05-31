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
data class AttendanceRecord(
    @Id
    @Column(columnDefinition = "uuid")
    val id: String = UUID.randomUUID().toString(),
    @Column(name = "employee_id", columnDefinition = "uuid")
    val employeeId: String,
    @Column(name = "work_date")
    val workDate: LocalDate,
    @Column(name = "clock_in")
    val clockIn: LocalDateTime? = null,
    @Column(name = "clock_out")
    val clockOut: LocalDateTime? = null,
    @Column(name = "worked_minutes")
    val workedMinutes: Int? = null,
    @Enumerated(EnumType.STRING)
    val status: AttendanceStatus = AttendanceStatus.PRESENT,
    val notes: String? = null,
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: String,
    @CreatedDate
    @Column(name = "created_at")
    val createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,
)
