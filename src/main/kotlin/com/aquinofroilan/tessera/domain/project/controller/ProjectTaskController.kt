package com.aquinofroilan.tessera.domain.project.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.project.dto.CreateProjectTaskRequest
import com.aquinofroilan.tessera.domain.project.dto.ProjectTaskResponse
import com.aquinofroilan.tessera.domain.project.dto.SetTaskParentRequest
import com.aquinofroilan.tessera.domain.project.dto.UpdateProjectTaskRequest
import com.aquinofroilan.tessera.domain.project.service.ProjectTaskService
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.security.CurrentOrganizationId
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
import java.util.UUID

@RestController
@RequestMapping("/api/v1/projects/{projectId}/tasks")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class ProjectTaskController(
    private val projectTaskService: ProjectTaskService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('projects:write')")
    fun createTask(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable projectId: java.util.UUID,
        @Valid @RequestBody request: CreateProjectTaskRequest,
    ): ResponseEntity<Any> {
        val created = projectTaskService.createTask(projectId, request, orgId)
        return ResponseEntity.status(HttpStatus.CREATED).body(ProjectTaskResponse.from(created))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('projects:read')")
    fun listTasks(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable projectId: java.util.UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(projectTaskService.listTasks(projectId, orgId).map { ProjectTaskResponse.from(it) })

    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('projects:read')")
    fun taskTree(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable projectId: java.util.UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(projectTaskService.getTaskTree(projectId, orgId))

    @GetMapping("/{taskId}")
    @PreAuthorize("hasAuthority('projects:read')")
    fun getTask(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable projectId: java.util.UUID,
        @PathVariable taskId: java.util.UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(ProjectTaskResponse.from(projectTaskService.getTask(projectId, taskId, orgId)))

    @PatchMapping("/{taskId}")
    @PreAuthorize("hasAuthority('projects:write')")
    fun updateTask(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable projectId: java.util.UUID,
        @PathVariable taskId: java.util.UUID,
        @Valid @RequestBody request: UpdateProjectTaskRequest,
    ): ResponseEntity<Any> = ResponseEntity.ok(ProjectTaskResponse.from(projectTaskService.updateTask(projectId, taskId, request, orgId)))

    @PutMapping("/{taskId}/parent")
    @PreAuthorize("hasAuthority('projects:write')")
    fun setParent(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable projectId: java.util.UUID,
        @PathVariable taskId: java.util.UUID,
        @Valid @RequestBody request: SetTaskParentRequest,
    ): ResponseEntity<Any> =
        ResponseEntity.ok(
            ProjectTaskResponse.from(projectTaskService.setParent(projectId, taskId, request.parentTaskId, orgId)),
        )
}
