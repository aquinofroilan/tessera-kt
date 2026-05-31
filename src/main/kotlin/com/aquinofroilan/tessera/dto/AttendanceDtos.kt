package com.aquinofroilan.tessera.dto

import com.aquinofroilan.tessera.model.AttendanceRecord
import com.aquinofroilan.tessera.model.AttendanceStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDate
import java.time.LocalDateTime

data class ClockRequest(
    @field:NotBlank(message = "Employee ID is required")
    val employeeId: String,
)

/**
 * Manual attendance entry/correction for a given day (e.g. recording an
 * absence or fixing clock times). [status] defaults to PRESENT when omitted.
 */
data class RecordAttendanceRequest(
    @field:NotBlank(message = "Employee ID is required")
    val employeeId: String,
    @field:NotNull(message = "Work date is required")
    val workDate: LocalDate?,
    val clockIn: LocalDateTime? = null,
    val clockOut: LocalDateTime? = null,
    val status: AttendanceStatus? = null,
    val notes: String? = null,
)

data class AttendanceResponse(
    val id: String,
    val employeeId: String,
    val workDate: String,
    val clockIn: String?,
    val clockOut: String?,
    val workedMinutes: Int?,
    val status: AttendanceStatus,
    val notes: String?,
    val organizationId: String,
    val createdAt: String?,
    val updatedAt: String?,
) {
    companion object {
        fun from(record: AttendanceRecord) =
            AttendanceResponse(
                id = record.id,
                employeeId = record.employeeId,
                workDate = record.workDate.toString(),
                clockIn = record.clockIn?.toString(),
                clockOut = record.clockOut?.toString(),
                workedMinutes = record.workedMinutes,
                status = record.status,
                notes = record.notes,
                organizationId = record.organizationId,
                createdAt = record.createdAt?.toString(),
                updatedAt = record.updatedAt?.toString(),
            )
    }
}
