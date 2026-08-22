package com.aquinofroilan.tessera.domain.project.service

import com.aquinofroilan.tessera.domain.hr.service.EmployeeService
import com.aquinofroilan.tessera.domain.project.dto.CreateProjectRequest
import com.aquinofroilan.tessera.domain.project.dto.UpdateProjectRequest
import com.aquinofroilan.tessera.domain.project.model.Project
import com.aquinofroilan.tessera.domain.project.model.ProjectStatus
import com.aquinofroilan.tessera.domain.project.repository.ProjectRepository
import com.aquinofroilan.tessera.domain.sales.model.Customer
import com.aquinofroilan.tessera.domain.sales.service.CustomerService
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
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

    private val orgId = java.util.UUID.fromString("e5628ca4-87a8-3e6f-8ae2-20213cc7ef92")
    private val start = LocalDate.of(2026, 1, 1)

    @BeforeEach
    fun setup() {
        repository = mock(ProjectRepository::class.java)
        customerService = mock(CustomerService::class.java)
        employeeService = mock(EmployeeService::class.java)
        whenever(repository.countByOrganizationId(orgId)).thenReturn(0L)
        whenever(repository.save(any<Project>())).thenAnswer { it.arguments[0] }
        whenever(
            customerService.getCustomer(java.util.UUID.fromString("3fee4eba-8bd0-3b22-b897-e836ed3ce230"), orgId),
        ).thenReturn(
            Customer(id = java.util.UUID.fromString("3fee4eba-8bd0-3b22-b897-e836ed3ce230"), name = "Globex", organizationId = orgId),
        )
        service = ProjectService(repository, customerService, employeeService)
    }

    private fun project(status: ProjectStatus = ProjectStatus.PLANNED) =
        Project(
            id = java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89"),
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
        whenever(
            customerService.getCustomer(java.util.UUID.fromString("b7f756f4-4e53-3047-b0aa-fd3012bea789"), orgId),
        ).thenThrow(ResourceNotFoundException("Customer not found"))
        assertThatThrownBy {
            service.createProject(
                CreateProjectRequest(
                    name = "Apollo",
                    startDate = start,
                    customerId = java.util.UUID.fromString("b7f756f4-4e53-3047-b0aa-fd3012bea789"),
                ),
                orgId,
            )
        }.isInstanceOf(ResourceNotFoundException::class.java)
    }

    @Test
    fun `activate moves planned to active and rejects from other states`() {
        whenever(
            repository.findById(java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89")),
        ).thenReturn(Optional.of(project(ProjectStatus.PLANNED)))
        assertThat(
            service.activateProject(java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89"), orgId).status,
        ).isEqualTo(ProjectStatus.ACTIVE)

        whenever(
            repository.findById(java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89")),
        ).thenReturn(Optional.of(project(ProjectStatus.CLOSED)))
        assertThatThrownBy {
            service.activateProject(java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89"), orgId)
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `hold requires an active project`() {
        whenever(
            repository.findById(java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89")),
        ).thenReturn(Optional.of(project(ProjectStatus.PLANNED)))
        assertThatThrownBy {
            service.holdProject(java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89"), orgId)
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `cancel rejects an already-closed project`() {
        whenever(
            repository.findById(java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89")),
        ).thenReturn(Optional.of(project(ProjectStatus.CLOSED)))
        assertThatThrownBy {
            service.cancelProject(java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89"), orgId)
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `update changes fields and validates end date`() {
        whenever(repository.findById(java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89"))).thenReturn(Optional.of(project()))
        val updated =
            service.updateProject(
                java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89"),
                UpdateProjectRequest(name = "Apollo II"),
                orgId,
            )
        assertThat(updated.name).isEqualTo("Apollo II")

        whenever(repository.findById(java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89"))).thenReturn(Optional.of(project()))
        assertThatThrownBy {
            service.updateProject(
                java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89"),
                UpdateProjectRequest(endDate = start.minusDays(5)),
                orgId,
            )
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `get rejects cross-org access`() {
        whenever(repository.findById(java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89"))).thenReturn(
            Optional.of(
                project().apply {
                    organizationId =
                        java.util.UUID.fromString("f022a845-ae01-3e07-ae04-7fc0ffb096a8")
                },
            ),
        )
        assertThatThrownBy {
            service.getProject(java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89"), orgId)
        }.isInstanceOf(ResourceNotFoundException::class.java)
    }
}
