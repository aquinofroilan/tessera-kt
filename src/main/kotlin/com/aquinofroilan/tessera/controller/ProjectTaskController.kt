package com.aquinofroilan.tessera.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.dto.CreateProjectTaskRequest
import com.aquinofroilan.tessera.dto.ProjectTaskResponse
import com.aquinofroilan.tessera.dto.SetTaskParentRequest
import com.aquinofroilan.tessera.dto.UpdateProjectTaskRequest
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.service.ProjectTaskService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/projects/{projectId}/tasks")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class ProjectTaskController(
    private val projectTaskService: ProjectTaskService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('projects:write')")
    fun createTask(
        @PathVariable projectId: java.util.UUID,
        @Valid @RequestBody request: CreateProjectTaskRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val created = projectTaskService.createTask(projectId, request, orgId)
        return ResponseEntity.status(HttpStatus.CREATED).body(ProjectTaskResponse.from(created))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('projects:read')")
    fun listTasks(
        @PathVariable projectId: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(projectTaskService.listTasks(projectId, orgId).map { ProjectTaskResponse.from(it) })
    }

    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('projects:read')")
    fun taskTree(
        @PathVariable projectId: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(projectTaskService.getTaskTree(projectId, orgId))
    }

    @GetMapping("/{taskId}")
    @PreAuthorize("hasAuthority('projects:read')")
    fun getTask(
        @PathVariable projectId: java.util.UUID,
        @PathVariable taskId: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(ProjectTaskResponse.from(projectTaskService.getTask(projectId, taskId, orgId)))
    }

    @PatchMapping("/{taskId}")
    @PreAuthorize("hasAuthority('projects:write')")
    fun updateTask(
        @PathVariable projectId: java.util.UUID,
        @PathVariable taskId: java.util.UUID,
        @Valid @RequestBody request: UpdateProjectTaskRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(ProjectTaskResponse.from(projectTaskService.updateTask(projectId, taskId, request, orgId)))
    }

    @PutMapping("/{taskId}/parent")
    @PreAuthorize("hasAuthority('projects:write')")
    fun setParent(
        @PathVariable projectId: java.util.UUID,
        @PathVariable taskId: java.util.UUID,
        @Valid @RequestBody request: SetTaskParentRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(
            ProjectTaskResponse.from(projectTaskService.setParent(projectId, taskId, request.parentTaskId, orgId)),
        )
    }
}
