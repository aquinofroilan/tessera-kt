package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.RecordAttendanceRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.AttendanceRecord
import com.aquinofroilan.tessera.model.AttendanceStatus
import com.aquinofroilan.tessera.model.Employee
import com.aquinofroilan.tessera.model.EmploymentStatus
import com.aquinofroilan.tessera.repository.AttendanceRecordRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Optional

class AttendanceServiceTest {
    private lateinit var repository: AttendanceRecordRepository
    private lateinit var employeeService: EmployeeService
    private lateinit var service: AttendanceService

    private val orgId = "org-1"
    private val empId = "e1"

    @BeforeEach
    fun setup() {
        repository = mock(AttendanceRecordRepository::class.java)
        employeeService = mock(EmployeeService::class.java)
        whenever(repository.save(any<AttendanceRecord>())).thenAnswer { it.arguments[0] }
        whenever(employeeService.getEmployee(empId, orgId)).thenReturn(employee())
        whenever(repository.findByOrganizationIdAndEmployeeIdAndWorkDate(any(), any(), any())).thenReturn(Optional.empty())
        service = AttendanceService(repository, employeeService)
    }

    private fun employee(status: EmploymentStatus = EmploymentStatus.ACTIVE) =
        Employee(
            id = empId,
            employeeNumber = "EMP-0001",
            firstName = "Ada",
            lastName = "Lovelace",
            hireDate = LocalDate.of(2020, 1, 1),
            status = status,
            organizationId = orgId,
        )

    @Test
    fun `clock-in creates a present record`() {
        val record = service.clockIn(empId, orgId)

        assertThat(record.clockIn).isNotNull()
        assertThat(record.clockOut).isNull()
        assertThat(record.status).isEqualTo(AttendanceStatus.PRESENT)
        assertThat(record.workDate).isEqualTo(LocalDate.now(ZoneOffset.UTC))
    }

    @Test
    fun `clock-in rejects a second clock-in the same day`() {
        whenever(repository.findByOrganizationIdAndEmployeeIdAndWorkDate(any(), any(), any()))
            .thenReturn(Optional.of(record(clockIn = LocalDateTime.now(ZoneOffset.UTC))))

        assertThatThrownBy { service.clockIn(empId, orgId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `clock-in is rejected for a terminated employee`() {
        whenever(employeeService.getEmployee(empId, orgId)).thenReturn(employee(EmploymentStatus.TERMINATED))

        assertThatThrownBy { service.clockIn(empId, orgId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `clock-out sets worked minutes`() {
        val start = LocalDateTime.now(ZoneOffset.UTC).minusHours(8)
        whenever(repository.findByOrganizationIdAndEmployeeIdAndWorkDate(any(), any(), any()))
            .thenReturn(Optional.of(record(clockIn = start)))

        val record = service.clockOut(empId, orgId)

        assertThat(record.clockOut).isNotNull()
        assertThat(record.workedMinutes).isGreaterThanOrEqualTo(8 * 60)
    }

    @Test
    fun `clock-out without a clock-in is rejected`() {
        assertThatThrownBy { service.clockOut(empId, orgId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `clock-out twice is rejected`() {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        whenever(repository.findByOrganizationIdAndEmployeeIdAndWorkDate(any(), any(), any()))
            .thenReturn(Optional.of(record(clockIn = now.minusHours(8), clockOut = now)))

        assertThatThrownBy { service.clockOut(empId, orgId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `manual entry computes worked minutes and defaults to present`() {
        val day = LocalDate.of(2026, 5, 1)
        val record =
            service.recordAttendance(
                RecordAttendanceRequest(
                    employeeId = empId,
                    workDate = day,
                    clockIn = day.atTime(9, 0),
                    clockOut = day.atTime(17, 30),
                ),
                orgId,
            )

        assertThat(record.status).isEqualTo(AttendanceStatus.PRESENT)
        assertThat(record.workedMinutes).isEqualTo(8 * 60 + 30)
    }

    @Test
    fun `manual entry rejects clock-out before clock-in`() {
        val day = LocalDate.of(2026, 5, 1)
        assertThatThrownBy {
            service.recordAttendance(
                RecordAttendanceRequest(
                    employeeId = empId,
                    workDate = day,
                    clockIn = day.atTime(17, 0),
                    clockOut = day.atTime(9, 0),
                ),
                orgId,
            )
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `get rejects cross-org access`() {
        whenever(repository.findById("a1"))
            .thenReturn(
                Optional.of(
                    record(clockIn = null).apply {
                        id = "a1"
                        organizationId = "other"
                    },
                ),
            )

        assertThatThrownBy { service.getAttendance("a1", orgId) }
            .isInstanceOf(ResourceNotFoundException::class.java)
    }

    @Test
    fun `timesheet rejects an inverted date range`() {
        assertThatThrownBy {
            service.listTimesheet(orgId, from = LocalDate.of(2026, 5, 10), to = LocalDate.of(2026, 5, 1))
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    private fun record(
        clockIn: LocalDateTime? = null,
        clockOut: LocalDateTime? = null,
    ) = AttendanceRecord(
        employeeId = empId,
        workDate = LocalDate.now(ZoneOffset.UTC),
        clockIn = clockIn,
        clockOut = clockOut,
        organizationId = orgId,
    )
}
