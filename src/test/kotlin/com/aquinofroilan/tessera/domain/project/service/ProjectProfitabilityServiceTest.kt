package com.aquinofroilan.tessera.domain.project.service

import com.aquinofroilan.tessera.domain.finance.dto.CreateJournalEntryRequest
import com.aquinofroilan.tessera.domain.finance.model.JournalEntry
import com.aquinofroilan.tessera.domain.finance.model.JournalEntryType
import com.aquinofroilan.tessera.domain.finance.service.JournalEntryService
import com.aquinofroilan.tessera.domain.hr.model.EmployeeCompensation
import com.aquinofroilan.tessera.domain.hr.model.PayPeriod
import com.aquinofroilan.tessera.domain.hr.service.EmployeeCompensationService
import com.aquinofroilan.tessera.domain.project.dto.RecognizeRevenueRequest
import com.aquinofroilan.tessera.domain.project.model.Project
import com.aquinofroilan.tessera.domain.project.model.ProjectBudget
import com.aquinofroilan.tessera.domain.project.model.ProjectCostCategory
import com.aquinofroilan.tessera.domain.project.model.ProjectRevenueRecognition
import com.aquinofroilan.tessera.domain.project.model.RevenueRecognitionMethod
import com.aquinofroilan.tessera.domain.project.model.TimeEntry
import com.aquinofroilan.tessera.domain.project.repository.ProjectBudgetRepository
import com.aquinofroilan.tessera.domain.project.repository.ProjectRevenueRecognitionRepository
import com.aquinofroilan.tessera.domain.project.repository.TimeEntryRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate

class ProjectProfitabilityServiceTest {
    private lateinit var projectService: ProjectService
    private lateinit var timeEntryRepository: TimeEntryRepository
    private lateinit var projectBudgetRepository: ProjectBudgetRepository
    private lateinit var employeeCompensationService: EmployeeCompensationService
    private lateinit var recognitionRepository: ProjectRevenueRecognitionRepository
    private lateinit var journalEntryService: JournalEntryService
    private lateinit var service: ProjectProfitabilityService

    private val orgId = java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d")
    private val projectId = java.util.UUID.fromString("297fd989-49e7-378b-9562-907dbf28876d")
    private val empId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val createdBy = java.util.UUID.fromString("00000000-0000-0000-0000-000000000002")
    private val wipAccountId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000003")
    private val revAccountId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000004")
    private val cosAccountId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000005")

    @BeforeEach
    fun setup() {
        projectService = mock(ProjectService::class.java)
        timeEntryRepository = mock(TimeEntryRepository::class.java)
        projectBudgetRepository = mock(ProjectBudgetRepository::class.java)
        employeeCompensationService = mock(EmployeeCompensationService::class.java)
        recognitionRepository = mock(ProjectRevenueRecognitionRepository::class.java)
        journalEntryService = mock(JournalEntryService::class.java)

        whenever(recognitionRepository.save(any<ProjectRevenueRecognition>())).thenAnswer { it.arguments[0] }
        whenever(journalEntryService.createJournalEntry(any<CreateJournalEntryRequest>(), any<java.util.UUID>(), any<java.util.UUID>()))
            .thenReturn(
                JournalEntry(
                    id = java.util.UUID.randomUUID(),
                    entryNumber = "JE-01",
                    date = LocalDate.now(),
                    description = "Test",
                    type = JournalEntryType.ACTUAL,
                    organizationId = orgId,
                    lines = emptyList(),
                    createdBy = createdBy,
                ),
            )

        service =
            ProjectProfitabilityService(
                projectService,
                timeEntryRepository,
                projectBudgetRepository,
                employeeCompensationService,
                recognitionRepository,
                journalEntryService,
            )
    }

    @Test
    fun `getProfitabilityReport computes percent complete based on costs`() {
        whenever(projectService.getProject(projectId, orgId)).thenReturn(
            Project(
                id = projectId,
                projectNumber = "PRJ-01",
                name = "Test",
                startDate = LocalDate.now(),
                organizationId = orgId,
                contractAmount = BigDecimal("10000.00"),
                revenueRecognitionMethod = RevenueRecognitionMethod.PERCENT_COMPLETE,
            ),
        )
        whenever(projectBudgetRepository.findByOrganizationIdAndProjectId(orgId, projectId)).thenReturn(
            listOf(
                ProjectBudget(
                    projectId = projectId,
                    category = ProjectCostCategory.LABOR,
                    budgetAmount = BigDecimal("5000.00"),
                    organizationId = orgId,
                ),
            ),
        )

        val te =
            TimeEntry(
                employeeId = empId,
                projectId = projectId,
                entryDate = LocalDate.now(),
                hours = BigDecimal("10.0"),
                organizationId = orgId,
                billable = false,
                invoiced = false,
            )
        whenever(
            timeEntryRepository.findByOrganizationIdAndProjectIdAndStatus(
                orgId,
                projectId,
                com.aquinofroilan.tessera.domain.project.model.TimeEntryStatus.APPROVED,
            ),
        ).thenReturn(listOf(te))

        whenever(employeeCompensationService.currentCompensationOrNull(empId, orgId, te.entryDate)).thenReturn(
            EmployeeCompensation(
                employeeId = empId,
                payRate = BigDecimal("100.0"),
                currency = "USD",
                payPeriod = PayPeriod.HOURLY,
                effectiveDate = LocalDate.now(),
                organizationId = orgId,
                createdBy = createdBy,
            ),
        )

        whenever(recognitionRepository.findByOrganizationIdAndProjectId(orgId, projectId)).thenReturn(emptyList())

        val report = service.getProfitabilityReport(projectId, orgId)

        assertThat(report.totalBudgetedCost).isEqualTo(BigDecimal("5000.00"))
        assertThat(report.actualCostToDate).isEqualTo(BigDecimal("1000.00")) // 10 hrs * 100
        assertThat(report.percentComplete).isEqualTo(BigDecimal("20.00"))
    }

    @Test
    fun `recognizeRevenue creates journal entry for incremental revenue`() {
        whenever(projectService.getProject(projectId, orgId)).thenReturn(
            Project(
                id = projectId,
                projectNumber = "PRJ-01",
                name = "Test",
                startDate = LocalDate.now(),
                organizationId = orgId,
                contractAmount = BigDecimal("10000.00"),
                revenueRecognitionMethod = RevenueRecognitionMethod.PERCENT_COMPLETE,
            ),
        )
        whenever(projectBudgetRepository.findByOrganizationIdAndProjectId(orgId, projectId)).thenReturn(
            listOf(
                ProjectBudget(
                    projectId = projectId,
                    category = ProjectCostCategory.LABOR,
                    budgetAmount = BigDecimal("5000.00"),
                    organizationId = orgId,
                ),
            ),
        )

        val te =
            TimeEntry(
                employeeId = empId,
                projectId = projectId,
                entryDate = LocalDate.now(),
                hours = BigDecimal("10.0"),
                organizationId = orgId,
                billable = false,
                invoiced = false,
            )
        whenever(
            timeEntryRepository.findByOrganizationIdAndProjectIdAndStatus(
                orgId,
                projectId,
                com.aquinofroilan.tessera.domain.project.model.TimeEntryStatus.APPROVED,
            ),
        ).thenReturn(listOf(te))
        whenever(employeeCompensationService.currentCompensationOrNull(empId, orgId, te.entryDate)).thenReturn(
            EmployeeCompensation(
                employeeId = empId,
                payRate = BigDecimal("100.0"),
                currency = "USD",
                payPeriod = PayPeriod.HOURLY,
                effectiveDate = LocalDate.now(),
                organizationId = orgId,
                createdBy = createdBy,
            ),
        )

        // Previously recognized 10% ($1000 rev, $500 cost)
        val priorRec =
            ProjectRevenueRecognition(
                organizationId = orgId,
                projectId = projectId,
                recognizedDate = LocalDate.now().minusDays(1),
                recognizedRevenue = BigDecimal("1000.0"),
                recognizedCost = BigDecimal("500.0"),
                createdBy = createdBy,
            )
        whenever(recognitionRepository.findByOrganizationIdAndProjectId(orgId, projectId)).thenReturn(listOf(priorRec))

        val req =
            RecognizeRevenueRequest(
                date = LocalDate.now(),
                wipAccountId = wipAccountId,
                revenueAccountId = revAccountId,
                costOfSalesAccountId = cosAccountId,
            )
        val response = service.recognizeRevenue(projectId, req, orgId, createdBy)

        // We are now 20% complete (2000 rev, 1000 cost). Incremental is 1000 rev, 500 cost.
        assertThat(response.recognizedRevenue.compareTo(BigDecimal("1000.00"))).isEqualTo(0)
        assertThat(response.recognizedCost.compareTo(BigDecimal("500.00"))).isEqualTo(0)
    }
}
