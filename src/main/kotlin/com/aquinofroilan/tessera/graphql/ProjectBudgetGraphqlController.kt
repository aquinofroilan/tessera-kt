package com.aquinofroilan.tessera.graphql

import com.aquinofroilan.tessera.controller.ProjectBudgetController
import com.aquinofroilan.tessera.dto.SetProjectBudgetRequest
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

/**
 * GraphQL bridge for project budgets, delegating to the REST controller via the
 * shared JSON-scalar pass-through. Authorization mirrors the REST endpoints
 * (`projects:read`/`projects:write`).
 */
@Controller
class ProjectBudgetGraphqlController(
    private val projectBudgetController: ProjectBudgetController,
    private val support: GraphqlBridgeSupport,
) {
    @QueryMapping
    @PreAuthorize("hasAuthority('projects:read')")
    fun projectBudgets(
        @Argument projectId: String,
    ): Any = support.unwrap(projectBudgetController.listBudgets(projectId))

    @QueryMapping
    @PreAuthorize("hasAuthority('projects:read')")
    fun projectBudgetVsActual(
        @Argument projectId: String,
    ): Any = support.unwrap(projectBudgetController.budgetVsActual(projectId))

    @MutationMapping
    @PreAuthorize("hasAuthority('projects:write')")
    fun setProjectBudget(
        @Argument projectId: String,
        @Argument input: Any,
    ): Any = support.unwrap(projectBudgetController.setBudget(projectId, support.toRequest<SetProjectBudgetRequest>(input)))
}
