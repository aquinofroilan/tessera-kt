package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.RecordAttendanceRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.AttendanceRecord
import com.aquinofroilan.tessera.model.AttendanceStatus
import com.aquinofroilan.tessera.model.EmploymentStatus
import com.aquinofroilan.tessera.repository.AttendanceRecordRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

@Service
class AttendanceService(
    private val attendanceRecordRepository: AttendanceRecordRepository,
    private val employeeService: EmployeeService,
) {
    @Transactional
    fun clockIn(
        employeeId: java.util.UUID,
        organizationId: java.util.UUID,
    ): AttendanceRecord {
        val employee = activeEmployee(employeeId, organizationId)
        val today = LocalDate.now(ZoneOffset.UTC)
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val existing = attendanceRecordRepository.findByOrganizationIdAndEmployeeIdAndWorkDate(organizationId, employee.id, today)
        if (existing.isPresent && existing.get().clockIn != null) {
            throw BusinessRuleException("Employee has already clocked in today")
        }
        val record =
            existing
                .map {
                    it.apply {
                        clockIn = now
                        status = AttendanceStatus.PRESENT
                    }
                }.orElseGet {
                    AttendanceRecord(
                        employeeId = employee.id,
                        workDate = today,
                        clockIn = now,
                        status = AttendanceStatus.PRESENT,
                        organizationId = organizationId,
                    )
                }
        return attendanceRecordRepository.save(record)
    }

    @Transactional
    fun clockOut(
        employeeId: java.util.UUID,
        organizationId: java.util.UUID,
    ): AttendanceRecord {
        val employee = activeEmployee(employeeId, organizationId)
        val today = LocalDate.now(ZoneOffset.UTC)
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val record =
            attendanceRecordRepository
                .findByOrganizationIdAndEmployeeIdAndWorkDate(organizationId, employee.id, today)
                .orElseThrow { BusinessRuleException("Employee has not clocked in today") }
        val clockIn = record.clockIn ?: throw BusinessRuleException("Employee has not clocked in today")
        if (record.clockOut != null) {
            throw BusinessRuleException("Employee has already clocked out today")
        }
        record.clockOut = now
        record.workedMinutes = ChronoUnit.MINUTES.between(clockIn, now).toInt()
        return attendanceRecordRepository.save(record)
    }

    /**
     * Manually records or corrects an attendance entry for a specific day,
     * upserting the unique (org, employee, date) record.
     */
    @Transactional
    fun recordAttendance(
        request: RecordAttendanceRequest,
        organizationId: java.util.UUID,
    ): AttendanceRecord {
        val workDate = request.workDate ?: throw BusinessRuleException("Work date is required")
        val employee = employeeService.getEmployee(request.employeeId, organizationId)
        if (request.clockIn != null && request.clockOut != null && request.clockOut.isBefore(request.clockIn)) {
            throw BusinessRuleException("Clock-out must be on or after clock-in")
        }
        val workedMinutes =
            if (request.clockIn != null && request.clockOut != null) {
                ChronoUnit.MINUTES.between(request.clockIn, request.clockOut).toInt()
            } else {
                null
            }
        val status = request.status ?: AttendanceStatus.PRESENT
        val existing =
            attendanceRecordRepository.findByOrganizationIdAndEmployeeIdAndWorkDate(organizationId, employee.id, workDate)
        val record =
            existing
                .map {
                    it.apply {
                        clockIn = request.clockIn
                        clockOut = request.clockOut
                        this.workedMinutes = workedMinutes
                        this.status = status
                        notes = request.notes
                    }
                }.orElseGet {
                    AttendanceRecord(
                        employeeId = employee.id,
                        workDate = workDate,
                        clockIn = request.clockIn,
                        clockOut = request.clockOut,
                        workedMinutes = workedMinutes,
                        status = status,
                        notes = request.notes,
                        organizationId = organizationId,
                    )
                }
        return attendanceRecordRepository.save(record)
    }

    fun getAttendance(
        id: java.util.UUID,
        organizationId: java.util.UUID,
    ): AttendanceRecord {
        val record =
            attendanceRecordRepository.findById(id).orElseThrow {
                ResourceNotFoundException("Attendance record not found")
            }
        if (record.organizationId != organizationId) {
            throw ResourceNotFoundException("Attendance record not found")
        }
        return record
    }

    /**
     * Returns a timesheet: attendance records for the org, optionally narrowed
     * by employee and/or an inclusive [from]..[to] date range.
     */
    fun listTimesheet(
        organizationId: java.util.UUID,
        employeeId: java.util.UUID? = null,
        from: LocalDate? = null,
        to: LocalDate? = null,
    ): List<AttendanceRecord> {
        if (from != null && to != null && to.isBefore(from)) {
            throw BusinessRuleException("'to' date must be on or after 'from' date")
        }
        return when {
            employeeId != null && from != null && to != null ->
                attendanceRecordRepository.findByOrganizationIdAndEmployeeIdAndWorkDateBetween(organizationId, employeeId, from, to)
            employeeId != null -> attendanceRecordRepository.findByOrganizationIdAndEmployeeId(organizationId, employeeId)
            from != null && to != null -> attendanceRecordRepository.findByOrganizationIdAndWorkDateBetween(organizationId, from, to)
            else -> attendanceRecordRepository.findByOrganizationId(organizationId)
        }
    }

    private fun activeEmployee(
        employeeId: java.util.UUID,
        organizationId: java.util.UUID,
    ) = employeeService.getEmployee(employeeId, organizationId).also {
        if (it.status == EmploymentStatus.TERMINATED) {
            throw BusinessRuleException("Cannot record attendance for a terminated employee")
        }
    }
}
