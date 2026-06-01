package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.AttendanceRecord
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.Optional

@Repository
interface AttendanceRecordRepository : JpaRepository<AttendanceRecord, String> {
    fun findByOrganizationId(organizationId: String): List<AttendanceRecord>

    fun findByOrganizationIdAndEmployeeId(
        organizationId: String,
        employeeId: String,
    ): List<AttendanceRecord>

    fun findByOrganizationIdAndEmployeeIdAndWorkDate(
        organizationId: String,
        employeeId: String,
        workDate: LocalDate,
    ): Optional<AttendanceRecord>

    fun findByOrganizationIdAndWorkDateBetween(
        organizationId: String,
        from: LocalDate,
        to: LocalDate,
    ): List<AttendanceRecord>

    fun findByOrganizationIdAndEmployeeIdAndWorkDateBetween(
        organizationId: String,
        employeeId: String,
        from: LocalDate,
        to: LocalDate,
    ): List<AttendanceRecord>
}
