package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateProjectRequest
import com.aquinofroilan.tessera.dto.UpdateProjectRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.Customer
import com.aquinofroilan.tessera.model.Project
import com.aquinofroilan.tessera.model.ProjectStatus
import com.aquinofroilan.tessera.repository.ProjectRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.Optional

class ProjectServiceTest {
    private lateinit var repository: ProjectRepository
    private lateinit var customerService: CustomerService
    private lateinit var employeeService: EmployeeService
    private lateinit var service: ProjectService

    private val orgId = "org-1"
    private val start = LocalDate.of(2026, 1, 1)

    @BeforeEach
    fun setup() {
        repository = mock(ProjectRepository::class.java)
        customerService = mock(CustomerService::class.java)
        employeeService = mock(EmployeeService::class.java)
        whenever(repository.countByOrganizationId(orgId)).thenReturn(0L)
        whenever(repository.save(any<Project>())).thenAnswer { it.arguments[0] }
        whenever(customerService.getCustomer("c-1", orgId)).thenReturn(Customer(id = "c-1", name = "Globex", organizationId = orgId))
        service = ProjectService(repository, customerService, employeeService)
    }

    private fun project(status: ProjectStatus = ProjectStatus.PLANNED) =
        Project(
            id = "p-1",
            projectNumber = "PRJ-0001",
            name = "Apollo",
            startDate = start,
            status = status,
            organizationId = orgId,
        )

    @Test
    fun `create persists a planned project with a generated number`() {
        val p = service.createProject(CreateProjectRequest(name = " Apollo ", startDate = start), orgId)

        assertThat(p.status).isEqualTo(ProjectStatus.PLANNED)
        assertThat(p.projectNumber).isEqualTo("PRJ-0001")
        assertThat(p.name).isEqualTo("Apollo")
    }

    @Test
    fun `create rejects an end date before the start date`() {
        assertThatThrownBy {
            service.createProject(CreateProjectRequest(name = "Apollo", startDate = start, endDate = start.minusDays(1)), orgId)
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `create validates the customer belongs to the org`() {
        whenever(customerService.getCustomer("missing", orgId)).thenThrow(ResourceNotFoundException("Customer not found"))
        assertThatThrownBy {
            service.createProject(CreateProjectRequest(name = "Apollo", startDate = start, customerId = "missing"), orgId)
        }.isInstanceOf(ResourceNotFoundException::class.java)
    }

    @Test
    fun `activate moves planned to active and rejects from other states`() {
        whenever(repository.findById("p-1")).thenReturn(Optional.of(project(ProjectStatus.PLANNED)))
        assertThat(service.activateProject("p-1", orgId).status).isEqualTo(ProjectStatus.ACTIVE)

        whenever(repository.findById("p-1")).thenReturn(Optional.of(project(ProjectStatus.CLOSED)))
        assertThatThrownBy { service.activateProject("p-1", orgId) }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `hold requires an active project`() {
        whenever(repository.findById("p-1")).thenReturn(Optional.of(project(ProjectStatus.PLANNED)))
        assertThatThrownBy { service.holdProject("p-1", orgId) }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `cancel rejects an already-closed project`() {
        whenever(repository.findById("p-1")).thenReturn(Optional.of(project(ProjectStatus.CLOSED)))
        assertThatThrownBy { service.cancelProject("p-1", orgId) }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `update changes fields and validates end date`() {
        whenever(repository.findById("p-1")).thenReturn(Optional.of(project()))
        val updated = service.updateProject("p-1", UpdateProjectRequest(name = "Apollo II"), orgId)
        assertThat(updated.name).isEqualTo("Apollo II")

        whenever(repository.findById("p-1")).thenReturn(Optional.of(project()))
        assertThatThrownBy {
            service.updateProject("p-1", UpdateProjectRequest(endDate = start.minusDays(5)), orgId)
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `get rejects cross-org access`() {
        whenever(repository.findById("p-1")).thenReturn(Optional.of(project().apply { organizationId = "other" }))
        assertThatThrownBy { service.getProject("p-1", orgId) }.isInstanceOf(ResourceNotFoundException::class.java)
    }
}
