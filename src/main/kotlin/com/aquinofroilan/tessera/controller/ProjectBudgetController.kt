package com.aquinofroilan.tessera.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.dto.ProjectBudgetResponse
import com.aquinofroilan.tessera.dto.SetProjectBudgetRequest
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.service.ProjectBudgetService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/projects/{projectId}/budget")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class ProjectBudgetController(
    private val projectBudgetService: ProjectBudgetService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('projects:write')")
    fun setBudget(
        @PathVariable projectId: String,
        @Valid @RequestBody request: SetProjectBudgetRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val budget = projectBudgetService.setBudget(projectId, request, orgId)
        return ResponseEntity.status(HttpStatus.CREATED).body(ProjectBudgetResponse.from(budget))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('projects:read')")
    fun listBudgets(
        @PathVariable projectId: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(projectBudgetService.listBudgets(projectId, orgId).map { ProjectBudgetResponse.from(it) })
    }

    @GetMapping("/vs-actual")
    @PreAuthorize("hasAuthority('projects:read')")
    fun budgetVsActual(
        @PathVariable projectId: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(projectBudgetService.budgetVsActual(projectId, orgId))
    }
}
