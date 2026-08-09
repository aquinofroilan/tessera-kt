package com.aquinofroilan.tessera.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.dto.CreateWorkflowRuleRequest
import com.aquinofroilan.tessera.dto.UpdateWorkflowRuleRequest
import com.aquinofroilan.tessera.dto.WorkflowRuleResponse
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.service.WorkflowRuleService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/workflow/rules")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class WorkflowRuleController(
    private val workflowRuleService: WorkflowRuleService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('workflow:manage')")
    fun createRule(
        @Valid @RequestBody request: CreateWorkflowRuleRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val created = workflowRuleService.createRule(request, orgId)
        return ResponseEntity.status(HttpStatus.CREATED).body(WorkflowRuleResponse.from(created))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('workflow:manage')")
    fun listRules(
        @RequestParam(required = false) eventKind: String?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(
            workflowRuleService.listRules(orgId, eventKind).map { WorkflowRuleResponse.from(it) },
        )
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('workflow:manage')")
    fun getRule(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(WorkflowRuleResponse.from(workflowRuleService.getRule(id, orgId)))
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('workflow:manage')")
    fun updateRule(
        @PathVariable id: java.util.UUID,
        @Valid @RequestBody request: UpdateWorkflowRuleRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(WorkflowRuleResponse.from(workflowRuleService.updateRule(id, orgId, request)))
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('workflow:manage')")
    fun deleteRule(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        workflowRuleService.deleteRule(id, orgId)
        return ResponseEntity.noContent().build()
    }
}
