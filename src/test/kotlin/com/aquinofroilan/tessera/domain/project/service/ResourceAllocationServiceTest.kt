package com.aquinofroilan.tessera.domain.project.service

import com.aquinofroilan.tessera.domain.hr.model.Employee
import com.aquinofroilan.tessera.domain.hr.service.EmployeeService
import com.aquinofroilan.tessera.domain.project.dto.CreateResourceAllocationRequest
import com.aquinofroilan.tessera.domain.project.model.Project
import com.aquinofroilan.tessera.domain.project.model.ProjectResourceAllocation
import com.aquinofroilan.tessera.domain.project.model.TimeEntry
import com.aquinofroilan.tessera.domain.project.repository.ProjectResourceAllocationRepository
import com.aquinofroilan.tessera.domain.project.repository.TimeEntryRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate

class ResourceAllocationServiceTest {
    private lateinit var allocationRepository: ProjectResourceAllocationRepository
    private lateinit var timeEntryRepository: TimeEntryRepository
    private lateinit var projectService: ProjectService
    private lateinit var employeeService: EmployeeService
    private lateinit var service: ResourceAllocationService

    private val orgId = java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d")
    private val projectId = java.util.UUID.fromString("297fd989-49e7-378b-9562-907dbf28876d")
    private val empId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001")

    @BeforeEach
    fun setup() {
        allocationRepository = mock(ProjectResourceAllocationRepository::class.java)
        timeEntryRepository = mock(TimeEntryRepository::class.java)
        projectService = mock(ProjectService::class.java)
        employeeService = mock(EmployeeService::class.java)

        whenever(allocationRepository.save(any<ProjectResourceAllocation>())).thenAnswer { it.arguments[0] }
        whenever(projectService.getProject(projectId, orgId)).thenReturn(
            Project(id = projectId, projectNumber = "PRJ-01", name = "Test", startDate = LocalDate.now(), organizationId = orgId),
        )
        whenever(employeeService.getEmployee(empId, orgId)).thenReturn(
            Employee(
                id = empId,
                employeeNumber = "EMP-01",
                firstName = "John",
                lastName = "Doe",
                email = "john@example.com",
                hireDate = LocalDate.now(),
                organizationId = orgId,
            ),
        )

        service = ResourceAllocationService(allocationRepository, timeEntryRepository, projectService, employeeService)
    }

    @Test
    fun `createAllocation persists an allocation`() {
        val req =
            CreateResourceAllocationRequest(
                employeeId = empId,
                startDate = LocalDate.of(2026, 1, 1),
                endDate = LocalDate.of(2026, 1, 31),
                allocatedHours = BigDecimal("40.0"),
            )
        val result = service.createAllocation(projectId, req, orgId)
        assertThat(result.allocatedHours).isEqualTo(BigDecimal("40.0"))
        assertThat(result.employeeId).isEqualTo(empId)
    }

    @Test
    fun `createAllocation rejects negative hours`() {
        val req =
            CreateResourceAllocationRequest(
                employeeId = empId,
                startDate = LocalDate.of(2026, 1, 1),
                endDate = LocalDate.of(2026, 1, 31),
                allocatedHours = BigDecimal("-10.0"),
            )
        assertThatThrownBy { service.createAllocation(projectId, req, orgId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `utilization report calculates correctly for overlapping date ranges`() {
        val alloc =
            ProjectResourceAllocation(
                organizationId = orgId,
                projectId = projectId,
                employeeId = empId,
                startDate = LocalDate.of(2026, 1, 1),
                endDate = LocalDate.of(2026, 1, 10), // 10 days
                allocatedHours = BigDecimal("100.0"),
            )
        whenever(allocationRepository.findByOrganizationIdAndEmployeeId(orgId, empId)).thenReturn(listOf(alloc))

        val timeEntry1 =
            TimeEntry(
                employeeId = empId,
                projectId = projectId,
                entryDate = LocalDate.of(2026, 1, 2),
                hours = BigDecimal("20.0"),
                organizationId = orgId,
            )
        val timeEntry2 =
            TimeEntry(
                employeeId = empId,
                projectId = projectId,
                entryDate = LocalDate.of(2026, 1, 3),
                hours = BigDecimal("30.0"),
                organizationId = orgId,
            )

        val reportStartDate = LocalDate.of(2026, 1, 1)
        val reportEndDate = LocalDate.of(2026, 1, 5) // 5 days overlap out of 10 days -> 50% allocated hours -> 50.0 hours

        whenever(
            timeEntryRepository.findByOrganizationIdAndEmployeeIdAndEntryDateBetween(
                orgId,
                empId,
                reportStartDate,
                reportEndDate,
            ),
        ).thenReturn(listOf(timeEntry1, timeEntry2)) // 50 actual hours

        val report = service.generateUtilizationReport(empId, reportStartDate, reportEndDate, orgId)

        assertThat(report.allocatedHours.compareTo(BigDecimal("50.00"))).isEqualTo(0)
        assertThat(report.actualHours.compareTo(BigDecimal("50.00"))).isEqualTo(0)
        assertThat(report.utilizationPercentage.compareTo(BigDecimal("100.00"))).isEqualTo(0)
    }
}
