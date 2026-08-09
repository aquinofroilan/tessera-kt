package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.AttendanceRecord
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.Optional

@Repository
interface AttendanceRecordRepository : JpaRepository<AttendanceRecord, java.util.UUID> {
    fun findByOrganizationId(organizationId: java.util.UUID): List<AttendanceRecord>

    fun findByOrganizationIdAndEmployeeId(
        organizationId: java.util.UUID,
        employeeId: java.util.UUID,
    ): List<AttendanceRecord>

    fun findByOrganizationIdAndEmployeeIdAndWorkDate(
        organizationId: java.util.UUID,
        employeeId: java.util.UUID,
        workDate: LocalDate,
    ): Optional<AttendanceRecord>

    fun findByOrganizationIdAndWorkDateBetween(
        organizationId: java.util.UUID,
        from: LocalDate,
        to: LocalDate,
    ): List<AttendanceRecord>

    fun findByOrganizationIdAndEmployeeIdAndWorkDateBetween(
        organizationId: java.util.UUID,
        employeeId: java.util.UUID,
        from: LocalDate,
        to: LocalDate,
    ): List<AttendanceRecord>
}
