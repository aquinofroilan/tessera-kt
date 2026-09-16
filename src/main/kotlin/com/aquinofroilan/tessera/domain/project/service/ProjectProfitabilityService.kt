package com.aquinofroilan.tessera.domain.project.service

import com.aquinofroilan.tessera.domain.finance.dto.CreateJournalEntryRequest
import com.aquinofroilan.tessera.domain.finance.dto.JournalEntryLineRequest
import com.aquinofroilan.tessera.domain.finance.service.JournalEntryService
import com.aquinofroilan.tessera.domain.hr.model.PayPeriod
import com.aquinofroilan.tessera.domain.hr.service.EmployeeCompensationService
import com.aquinofroilan.tessera.domain.project.dto.ProjectProfitabilityReport
import com.aquinofroilan.tessera.domain.project.dto.ProjectRevenueRecognitionResponse
import com.aquinofroilan.tessera.domain.project.dto.RecognizeRevenueRequest
import com.aquinofroilan.tessera.domain.project.model.ProjectRevenueRecognition
import com.aquinofroilan.tessera.domain.project.model.RevenueRecognitionMethod
import com.aquinofroilan.tessera.domain.project.model.TimeEntryStatus
import com.aquinofroilan.tessera.domain.project.repository.ProjectBudgetRepository
import com.aquinofroilan.tessera.domain.project.repository.ProjectRevenueRecognitionRepository
import com.aquinofroilan.tessera.domain.project.repository.TimeEntryRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

@Service
class ProjectProfitabilityService(
    private val projectService: ProjectService,
    private val timeEntryRepository: TimeEntryRepository,
    private val projectBudgetRepository: ProjectBudgetRepository,
    private val employeeCompensationService: EmployeeCompensationService,
    private val recognitionRepository: ProjectRevenueRecognitionRepository,
    private val journalEntryService: JournalEntryService,
) {
    fun getProfitabilityReport(
        projectId: UUID,
        organizationId: UUID,
    ): ProjectProfitabilityReport {
        val project = projectService.getProject(projectId, organizationId)

        // 1. Expected Revenue
        val totalExpectedRevenue = project.contractAmount ?: BigDecimal.ZERO

        // 2. Budgeted Cost
        val budgets = projectBudgetRepository.findByOrganizationIdAndProjectId(organizationId, projectId)
        val totalBudgetedCost = budgets.map { it.budgetAmount }.fold(BigDecimal.ZERO, BigDecimal::add)

        // 3. Actual Cost to Date
        val timeEntries = timeEntryRepository.findByOrganizationIdAndProjectIdAndStatus(organizationId, projectId, TimeEntryStatus.APPROVED)
        var actualCostToDate = BigDecimal.ZERO
        var actualBilledToDate = BigDecimal.ZERO

        for (entry in timeEntries) {
            val comp = employeeCompensationService.currentCompensationOrNull(entry.employeeId, organizationId, entry.entryDate)
            val hourlyCost =
                comp?.let {
                    when (it.payPeriod) {
                        PayPeriod.HOURLY -> it.payRate
                        PayPeriod.ANNUAL -> it.payRate.divide(BigDecimal("2080"), 4, RoundingMode.HALF_UP)
                        PayPeriod.MONTHLY -> it.payRate.divide(BigDecimal("173.33"), 4, RoundingMode.HALF_UP)
                    }
                } ?: BigDecimal.ZERO

            actualCostToDate = actualCostToDate.add(entry.hours.multiply(hourlyCost))

            if (entry.invoiced && entry.billable) {
                actualBilledToDate = actualBilledToDate.add(entry.hours.multiply(entry.rate ?: BigDecimal.ZERO))
            }
        }

        // 4. Recognized Revenue so far
        val recognitions = recognitionRepository.findByOrganizationIdAndProjectId(organizationId, projectId)
        val totalRecognizedRevenue = recognitions.map { it.recognizedRevenue }.fold(BigDecimal.ZERO, BigDecimal::add)
        val totalRecognizedCost = recognitions.map { it.recognizedCost }.fold(BigDecimal.ZERO, BigDecimal::add)

        // 5. Percent Complete (Cost-to-Cost)
        val percentComplete =
            if (totalBudgetedCost > BigDecimal.ZERO) {
                actualCostToDate.multiply(BigDecimal("100")).divide(totalBudgetedCost, 2, RoundingMode.HALF_UP)
            } else {
                BigDecimal.ZERO
            }

        // 6. Margin = Recognized Revenue - Actual Cost
        val margin = totalRecognizedRevenue.subtract(actualCostToDate)

        return ProjectProfitabilityReport(
            projectId = projectId,
            totalExpectedRevenue = totalExpectedRevenue,
            totalBudgetedCost = totalBudgetedCost,
            actualCostToDate = actualCostToDate,
            actualBilledToDate = actualBilledToDate,
            totalRecognizedRevenue = totalRecognizedRevenue,
            totalRecognizedCost = totalRecognizedCost,
            percentComplete = percentComplete,
            margin = margin,
        )
    }

    @Transactional
    fun recognizeRevenue(
        projectId: UUID,
        request: RecognizeRevenueRequest,
        organizationId: UUID,
        createdBy: UUID,
    ): ProjectRevenueRecognitionResponse {
        val project = projectService.getProject(projectId, organizationId)
        val method = project.revenueRecognitionMethod ?: throw BusinessRuleException("Project has no revenue recognition method defined")
        if (method == RevenueRecognitionMethod.NONE) {
            throw BusinessRuleException("Revenue recognition is disabled for this project")
        }

        val wipAccountId = request.wipAccountId ?: throw BusinessRuleException("WIP Account ID is required")
        val revenueAccountId = request.revenueAccountId ?: throw BusinessRuleException("Revenue Account ID is required")
        val costOfSalesAccountId = request.costOfSalesAccountId ?: throw BusinessRuleException("Cost of Sales Account ID is required")

        val report = getProfitabilityReport(projectId, organizationId)

        var cumulativeRevenue = BigDecimal.ZERO
        var cumulativeCost = BigDecimal.ZERO

        if (method == RevenueRecognitionMethod.PERCENT_COMPLETE) {
            if (report.percentComplete > BigDecimal("100")) {
                cumulativeRevenue = report.totalExpectedRevenue
                cumulativeCost = report.actualCostToDate
            } else {
                cumulativeRevenue =
                    report.totalExpectedRevenue.multiply(report.percentComplete).divide(BigDecimal("100"), 2, RoundingMode.HALF_UP)
                cumulativeCost = report.actualCostToDate
            }
        } else if (method == RevenueRecognitionMethod.COMPLETED_CONTRACT) {
            // Only recognize if 100% complete (for this simplified example)
            if (report.percentComplete >= BigDecimal("100")) {
                cumulativeRevenue = report.totalExpectedRevenue
                cumulativeCost = report.actualCostToDate
            }
        }

        val incrementalRevenue = cumulativeRevenue.subtract(report.totalRecognizedRevenue)
        val incrementalCost = cumulativeCost.subtract(report.totalRecognizedCost)

        if (incrementalRevenue.compareTo(BigDecimal.ZERO) <= 0 && incrementalCost.compareTo(BigDecimal.ZERO) <= 0) {
            throw BusinessRuleException("No incremental revenue or cost to recognize")
        }

        val date = request.date ?: java.time.LocalDate.now()

        val lines = mutableListOf<JournalEntryLineRequest>()

        if (incrementalRevenue > BigDecimal.ZERO) {
            lines.add(
                JournalEntryLineRequest(
                    accountId = wipAccountId,
                    debit = incrementalRevenue,
                    description = "WIP (Unbilled AR) - ${project.projectNumber}",
                ),
            )
            lines.add(
                JournalEntryLineRequest(
                    accountId = revenueAccountId,
                    credit = incrementalRevenue,
                    description = "Recognized Revenue - ${project.projectNumber}",
                ),
            )
        }

        if (incrementalCost > BigDecimal.ZERO) {
            lines.add(
                JournalEntryLineRequest(
                    accountId = costOfSalesAccountId,
                    debit = incrementalCost,
                    description = "Cost of Sales - ${project.projectNumber}",
                ),
            )
            lines.add(
                JournalEntryLineRequest(
                    accountId = wipAccountId,
                    credit = incrementalCost,
                    description = "WIP (Cost Relief) - ${project.projectNumber}",
                ),
            )
        }

        val je =
            journalEntryService.createJournalEntry(
                CreateJournalEntryRequest(
                    date = date,
                    description = "Revenue Recognition - ${project.projectNumber}",
                    lines = lines,
                    sourceReference = "PRJ-${project.id}",
                ),
                organizationId,
                createdBy,
            )

        val recognition =
            ProjectRevenueRecognition(
                organizationId = organizationId,
                projectId = projectId,
                recognizedDate = date,
                recognizedRevenue = incrementalRevenue,
                recognizedCost = incrementalCost,
                percentComplete = report.percentComplete,
                journalEntryId = je.id,
                createdBy = createdBy,
            )
        val saved = recognitionRepository.save(recognition)

        return ProjectRevenueRecognitionResponse(
            id = saved.id,
            projectId = saved.projectId,
            recognizedDate = saved.recognizedDate,
            recognizedRevenue = saved.recognizedRevenue,
            recognizedCost = saved.recognizedCost,
            percentComplete = saved.percentComplete,
            journalEntryId = saved.journalEntryId,
            organizationId = saved.organizationId,
            createdBy = saved.createdBy,
            createdAt = saved.createdAt?.toString(),
        )
    }
}
