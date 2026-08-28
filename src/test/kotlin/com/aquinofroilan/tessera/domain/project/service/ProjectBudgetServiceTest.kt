package com.aquinofroilan.tessera.domain.project.service

import com.aquinofroilan.tessera.domain.project.dto.SetProjectBudgetRequest
import com.aquinofroilan.tessera.domain.project.model.Project
import com.aquinofroilan.tessera.domain.project.model.ProjectBudget
import com.aquinofroilan.tessera.domain.project.model.ProjectCostCategory
import com.aquinofroilan.tessera.domain.project.model.TimeEntry
import com.aquinofroilan.tessera.domain.project.model.TimeEntryStatus
import com.aquinofroilan.tessera.domain.project.repository.ProjectBudgetRepository
import com.aquinofroilan.tessera.domain.project.repository.TimeEntryRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

class ProjectBudgetServiceTest {
    private lateinit var repository: ProjectBudgetRepository
    private lateinit var projectService: ProjectService
    private lateinit var timeEntryRepository: TimeEntryRepository
    private lateinit var service: ProjectBudgetService

    private val orgId = java.util.UUID.fromString("e5628ca4-87a8-3e6f-8ae2-20213cc7ef92")
    private val projectId = java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89")
    private val day = LocalDate.of(2026, 5, 1)

    @BeforeEach
    fun setup() {
        repository = mock(ProjectBudgetRepository::class.java)
        projectService = mock(ProjectService::class.java)
        timeEntryRepository = mock(TimeEntryRepository::class.java)
        whenever(repository.save(any<ProjectBudget>())).thenAnswer { it.arguments[0] }
        whenever(projectService.getProject(projectId, orgId)).thenReturn(
            Project(id = projectId, projectNumber = "PRJ-0001", name = "Apollo", startDate = day, organizationId = orgId),
        )
        whenever(repository.findByOrganizationIdAndProjectIdAndCategory(any(), any(), any())).thenReturn(Optional.empty())
        whenever(timeEntryRepository.findByOrganizationIdAndProjectIdAndStatus(any(), any(), any())).thenReturn(emptyList())
        service = ProjectBudgetService(repository, projectService, timeEntryRepository)
    }

    private fun entry(
        hours: String,
        rate: String?,
    ) = TimeEntry(
        employeeId = java.util.UUID.fromString("00262aa5-14d7-3a01-b098-d7e370f001b2"),
        projectId = projectId,
        entryDate = day,
        hours = BigDecimal(hours),
        rate = rate?.let { BigDecimal(it) },
        status = TimeEntryStatus.APPROVED,
        organizationId = orgId,
    )

    @Test
    fun `set budget creates a new category budget`() {
        val b = service.setBudget(projectId, SetProjectBudgetRequest(ProjectCostCategory.LABOR, BigDecimal("1000")), orgId)
        assertThat(b.category).isEqualTo(ProjectCostCategory.LABOR)
        assertThat(b.budgetAmount).isEqualByComparingTo("1000")
    }

    @Test
    fun `set budget updates an existing category budget`() {
        whenever(repository.findByOrganizationIdAndProjectIdAndCategory(orgId, projectId, ProjectCostCategory.LABOR))
            .thenReturn(
                Optional.of(
                    ProjectBudget(
                        id = java.util.UUID.fromString("0450a282-dc1c-3366-95d5-db2755646dac"),
                        projectId = projectId,
                        category = ProjectCostCategory.LABOR,
                        budgetAmount = BigDecimal("500"),
                        organizationId = orgId,
                    ),
                ),
            )
        val b = service.setBudget(projectId, SetProjectBudgetRequest(ProjectCostCategory.LABOR, BigDecimal("1500")), orgId)
        assertThat(b.id).isEqualTo(java.util.UUID.fromString("0450a282-dc1c-3366-95d5-db2755646dac"))
        assertThat(b.budgetAmount).isEqualByComparingTo("1500")
    }

    @Test
    fun `budget vs actual computes labor actual from approved time entries`() {
        whenever(repository.findByOrganizationIdAndProjectId(orgId, projectId)).thenReturn(
            listOf(
                ProjectBudget(
                    projectId = projectId,
                    category = ProjectCostCategory.LABOR,
                    budgetAmount = BigDecimal("1000"),
                    organizationId = orgId,
                ),
            ),
        )
        whenever(timeEntryRepository.findByOrganizationIdAndProjectIdAndStatus(orgId, projectId, TimeEntryStatus.APPROVED))
            .thenReturn(listOf(entry("4", "50"), entry("4", "50"), entry("2", null)))

        val report = service.budgetVsActual(projectId, orgId)

        val labor = report.lines.first { it.category == ProjectCostCategory.LABOR }
        assertThat(labor.budgeted).isEqualByComparingTo("1000")
        assertThat(labor.actual).isEqualByComparingTo("400") // 4*50 + 4*50; the null-rate entry is excluded
        assertThat(labor.remaining).isEqualByComparingTo("600")
        assertThat(report.totalActual).isEqualByComparingTo("400")
    }
}
