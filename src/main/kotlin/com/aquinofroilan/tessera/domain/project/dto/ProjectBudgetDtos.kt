package com.aquinofroilan.tessera.domain.project.dto

import com.aquinofroilan.tessera.domain.project.model.ProjectBudget
import com.aquinofroilan.tessera.domain.project.model.ProjectCostCategory
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

data class SetProjectBudgetRequest(
    @field:NotNull(message = "Category is required")
    val category: ProjectCostCategory?,
    @field:NotNull(message = "Budget amount is required")
    val budgetAmount: BigDecimal?,
    val currency: String? = null,
)

data class ProjectBudgetResponse(
    val id: java.util.UUID,
    val projectId: java.util.UUID,
    val category: ProjectCostCategory,
    val budgetAmount: BigDecimal,
    val currency: String?,
    val organizationId: java.util.UUID,
    val createdAt: String?,
    val updatedAt: String?,
) {
    companion object {
        fun from(budget: ProjectBudget) =
            ProjectBudgetResponse(
                id = budget.id,
                projectId = budget.projectId,
                category = budget.category,
                budgetAmount = budget.budgetAmount,
                currency = budget.currency,
                organizationId = budget.organizationId,
                createdAt = budget.createdAt?.toString(),
                updatedAt = budget.updatedAt?.toString(),
            )
    }
}

data class BudgetVarianceLine(
    val category: ProjectCostCategory,
    val budgeted: BigDecimal,
    val actual: BigDecimal,
    val remaining: BigDecimal,
)

data class ProjectBudgetVsActualResponse(
    val projectId: java.util.UUID,
    val lines: List<BudgetVarianceLine>,
    val totalBudgeted: BigDecimal,
    val totalActual: BigDecimal,
    val totalRemaining: BigDecimal,
)
