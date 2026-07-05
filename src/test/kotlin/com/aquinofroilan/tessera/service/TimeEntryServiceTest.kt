package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateTimeEntryRequest
import com.aquinofroilan.tessera.dto.UpdateTimeEntryRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.Employee
import com.aquinofroilan.tessera.model.Project
import com.aquinofroilan.tessera.model.TimeEntry
import com.aquinofroilan.tessera.model.TimeEntryStatus
import com.aquinofroilan.tessera.repository.TimeEntryRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

class TimeEntryServiceTest {
    private lateinit var repository: TimeEntryRepository
    private lateinit var projectService: ProjectService
    private lateinit var projectTaskService: ProjectTaskService
    private lateinit var employeeService: EmployeeService
    private lateinit var service: TimeEntryService

    private val orgId = "org-1"
    private val projectId = "p-1"
    private val empId = "e-1"
    private val day = LocalDate.of(2026, 5, 1)

    @BeforeEach
    fun setup() {
        repository = mock(TimeEntryRepository::class.java)
        projectService = mock(ProjectService::class.java)
        projectTaskService = mock(ProjectTaskService::class.java)
        employeeService = mock(EmployeeService::class.java)
        whenever(repository.save(any<TimeEntry>())).thenAnswer { it.arguments[0] }
        whenever(projectService.getProject(projectId, orgId)).thenReturn(
            Project(id = projectId, projectNumber = "PRJ-0001", name = "Apollo", startDate = day, organizationId = orgId),
        )
        whenever(employeeService.getEmployee(empId, orgId)).thenReturn(
            Employee(
                id = empId,
                employeeNumber = "EMP-0001",
                firstName = "Ada",
                lastName = "Lovelace",
                hireDate = day,
                organizationId = orgId,
            ),
        )
        service = TimeEntryService(repository, projectService, projectTaskService, employeeService)
    }

    private fun req(hours: BigDecimal = BigDecimal("4")) =
        CreateTimeEntryRequest(employeeId = empId, projectId = projectId, entryDate = day, hours = hours)

    private fun entry(status: TimeEntryStatus = TimeEntryStatus.DRAFT) =
        TimeEntry(
            id = "te-1",
            employeeId = empId,
            projectId = projectId,
            entryDate = day,
            hours = BigDecimal("4"),
            status = status,
            organizationId = orgId,
        )

    @Test
    fun `create persists a draft entry`() {
        val e = service.createTimeEntry(req(), orgId)
        assertThat(e.status).isEqualTo(TimeEntryStatus.DRAFT)
        assertThat(e.hours).isEqualByComparingTo("4")
    }

    @Test
    fun `create rejects non-positive hours`() {
        assertThatThrownBy { service.createTimeEntry(req(hours = BigDecimal.ZERO), orgId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `submit moves draft to submitted then approval is gated`() {
        whenever(repository.findById("te-1")).thenReturn(Optional.of(entry(TimeEntryStatus.DRAFT)))
        assertThat(service.submitTimeEntry("te-1", orgId).status).isEqualTo(TimeEntryStatus.SUBMITTED)

        whenever(repository.findById("te-1")).thenReturn(Optional.of(entry(TimeEntryStatus.DRAFT)))
        assertThatThrownBy { service.approveTimeEntry("te-1", orgId, "u1") }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `approve sets approver on a submitted entry`() {
        whenever(repository.findById("te-1")).thenReturn(Optional.of(entry(TimeEntryStatus.SUBMITTED)))
        val approved = service.approveTimeEntry("te-1", orgId, "u1")
        assertThat(approved.status).isEqualTo(TimeEntryStatus.APPROVED)
        assertThat(approved.approvedBy).isEqualTo("u1")
    }

    @Test
    fun `update is rejected once not draft`() {
        whenever(repository.findById("te-1")).thenReturn(Optional.of(entry(TimeEntryStatus.APPROVED)))
        assertThatThrownBy { service.updateTimeEntry("te-1", UpdateTimeEntryRequest(hours = BigDecimal("2")), orgId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `get rejects cross-org access`() {
        whenever(repository.findById("te-1")).thenReturn(Optional.of(entry().apply { organizationId = "other" }))
        assertThatThrownBy { service.getTimeEntry("te-1", orgId) }
            .isInstanceOf(ResourceNotFoundException::class.java)
    }
}
