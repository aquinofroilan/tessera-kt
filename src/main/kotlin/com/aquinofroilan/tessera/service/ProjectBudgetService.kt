package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.BudgetVarianceLine
import com.aquinofroilan.tessera.dto.ProjectBudgetVsActualResponse
import com.aquinofroilan.tessera.dto.SetProjectBudgetRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.model.ProjectBudget
import com.aquinofroilan.tessera.model.ProjectCostCategory
import com.aquinofroilan.tessera.model.TimeEntryStatus
import com.aquinofroilan.tessera.repository.ProjectBudgetRepository
import com.aquinofroilan.tessera.repository.TimeEntryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class ProjectBudgetService(
    private val projectBudgetRepository: ProjectBudgetRepository,
    private val projectService: ProjectService,
    private val timeEntryRepository: TimeEntryRepository,
) {
    /** Upserts the budget for a single cost category on a project. */
    @Transactional
    fun setBudget(
        projectId: java.util.UUID,
        request: SetProjectBudgetRequest,
        organizationId: java.util.UUID,
    ): ProjectBudget {
        projectService.getProject(projectId, organizationId)
        val category = request.category ?: throw BusinessRuleException("Category is required")
        val amount = request.budgetAmount ?: throw BusinessRuleException("Budget amount is required")
        if (amount.signum() < 0) {
            throw BusinessRuleException("Budget amount must not be negative")
        }
        val existing =
            projectBudgetRepository.findByOrganizationIdAndProjectIdAndCategory(organizationId, projectId, category)
        val budget =
            existing
                .map {
                    it.apply {
                        budgetAmount = amount
                        currency = request.currency ?: it.currency
                    }
                }.orElseGet {
                    ProjectBudget(
                        projectId = projectId,
                        category = category,
                        budgetAmount = amount,
                        currency = request.currency,
                        organizationId = organizationId,
                    )
                }
        return projectBudgetRepository.save(budget)
    }

    fun listBudgets(
        projectId: java.util.UUID,
        organizationId: java.util.UUID,
    ): List<ProjectBudget> {
        projectService.getProject(projectId, organizationId)
        return projectBudgetRepository.findByOrganizationIdAndProjectId(organizationId, projectId)
    }

    /**
     * Budget vs actual for a project. Labor actuals are the sum of (hours x rate)
     * over approved time entries. Material/expense actuals are 0 until bills and
     * expense claims carry a project reference (Epic #166).
     */
    fun budgetVsActual(
        projectId: java.util.UUID,
        organizationId: java.util.UUID,
    ): ProjectBudgetVsActualResponse {
        projectService.getProject(projectId, organizationId)
        val budgets = projectBudgetRepository.findByOrganizationIdAndProjectId(organizationId, projectId)

        val laborActual =
            timeEntryRepository
                .findByOrganizationIdAndProjectIdAndStatus(organizationId, projectId, TimeEntryStatus.APPROVED)
                .filter { it.rate != null }
                .fold(BigDecimal.ZERO) { sum, entry -> sum.add(entry.hours.multiply(entry.rate)) }

        val actualByCategory = mapOf(ProjectCostCategory.LABOR to laborActual)
        val budgetByCategory = budgets.associateBy { it.category }
        val categories =
            (budgetByCategory.keys + actualByCategory.filterValues { it.signum() != 0 }.keys)
                .distinct()
                .sortedBy { it.name }

        val lines =
            categories.map { category ->
                val budgeted = budgetByCategory[category]?.budgetAmount ?: BigDecimal.ZERO
                val actual = actualByCategory[category] ?: BigDecimal.ZERO
                BudgetVarianceLine(
                    category = category,
                    budgeted = budgeted,
                    actual = actual,
                    remaining = budgeted.subtract(actual),
                )
            }

        return ProjectBudgetVsActualResponse(
            projectId = projectId,
            lines = lines,
            totalBudgeted = lines.fold(BigDecimal.ZERO) { s, l -> s.add(l.budgeted) },
            totalActual = lines.fold(BigDecimal.ZERO) { s, l -> s.add(l.actual) },
            totalRemaining = lines.fold(BigDecimal.ZERO) { s, l -> s.add(l.remaining) },
        )
    }
}
