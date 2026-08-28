package com.aquinofroilan.tessera.domain.project.service

import com.aquinofroilan.tessera.domain.hr.model.Employee
import com.aquinofroilan.tessera.domain.hr.service.EmployeeService
import com.aquinofroilan.tessera.domain.project.dto.CreateTimeEntryRequest
import com.aquinofroilan.tessera.domain.project.dto.UpdateTimeEntryRequest
import com.aquinofroilan.tessera.domain.project.model.Project
import com.aquinofroilan.tessera.domain.project.model.TimeEntry
import com.aquinofroilan.tessera.domain.project.model.TimeEntryStatus
import com.aquinofroilan.tessera.domain.project.repository.TimeEntryRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
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

    private val orgId = java.util.UUID.fromString("e5628ca4-87a8-3e6f-8ae2-20213cc7ef92")
    private val projectId = java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89")
    private val empId = java.util.UUID.fromString("00262aa5-14d7-3a01-b098-d7e370f001b2")
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
            id = java.util.UUID.fromString("dbd39088-81c8-3352-845f-b26779415ecf"),
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
        whenever(
            repository.findById(java.util.UUID.fromString("dbd39088-81c8-3352-845f-b26779415ecf")),
        ).thenReturn(Optional.of(entry(TimeEntryStatus.DRAFT)))
        assertThat(
            service.submitTimeEntry(java.util.UUID.fromString("dbd39088-81c8-3352-845f-b26779415ecf"), orgId).status,
        ).isEqualTo(TimeEntryStatus.SUBMITTED)

        whenever(
            repository.findById(java.util.UUID.fromString("dbd39088-81c8-3352-845f-b26779415ecf")),
        ).thenReturn(Optional.of(entry(TimeEntryStatus.DRAFT)))
        assertThatThrownBy {
            service.approveTimeEntry(
                java.util.UUID.fromString("dbd39088-81c8-3352-845f-b26779415ecf"),
                orgId,
                java.util.UUID.fromString("d4763ac6-a6a6-34ed-aeb4-dd91bdcf7fbb"),
            )
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `approve sets approver on a submitted entry`() {
        whenever(
            repository.findById(java.util.UUID.fromString("dbd39088-81c8-3352-845f-b26779415ecf")),
        ).thenReturn(Optional.of(entry(TimeEntryStatus.SUBMITTED)))
        val approved =
            service.approveTimeEntry(
                java.util.UUID.fromString("dbd39088-81c8-3352-845f-b26779415ecf"),
                orgId,
                java.util.UUID.fromString("d4763ac6-a6a6-34ed-aeb4-dd91bdcf7fbb"),
            )
        assertThat(approved.status).isEqualTo(TimeEntryStatus.APPROVED)
        assertThat(approved.approvedBy).isEqualTo(java.util.UUID.fromString("d4763ac6-a6a6-34ed-aeb4-dd91bdcf7fbb"))
    }

    @Test
    fun `update is rejected once not draft`() {
        whenever(
            repository.findById(java.util.UUID.fromString("dbd39088-81c8-3352-845f-b26779415ecf")),
        ).thenReturn(Optional.of(entry(TimeEntryStatus.APPROVED)))
        assertThatThrownBy {
            service.updateTimeEntry(
                java.util.UUID.fromString("dbd39088-81c8-3352-845f-b26779415ecf"),
                UpdateTimeEntryRequest(hours = BigDecimal("2")),
                orgId,
            )
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `get rejects cross-org access`() {
        whenever(repository.findById(java.util.UUID.fromString("dbd39088-81c8-3352-845f-b26779415ecf"))).thenReturn(
            Optional.of(
                entry().apply {
                    organizationId =
                        java.util.UUID.fromString("f022a845-ae01-3e07-ae04-7fc0ffb096a8")
                },
            ),
        )
        assertThatThrownBy { service.getTimeEntry(java.util.UUID.fromString("dbd39088-81c8-3352-845f-b26779415ecf"), orgId) }
            .isInstanceOf(ResourceNotFoundException::class.java)
    }
}
