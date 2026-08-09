package com.aquinofroilan.tessera.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.dto.CreateProjectRequest
import com.aquinofroilan.tessera.dto.ProjectResponse
import com.aquinofroilan.tessera.dto.UpdateProjectRequest
import com.aquinofroilan.tessera.model.ProjectStatus
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.service.ProjectService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.Locale

@RestController
@RequestMapping("/projects")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class ProjectController(
    private val projectService: ProjectService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('projects:write')")
    fun createProject(
        @Valid @RequestBody request: CreateProjectRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val created = projectService.createProject(request, orgId)
        return ResponseEntity.status(HttpStatus.CREATED).body(ProjectResponse.from(created))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('projects:read')")
    fun listProjects(
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) customerId: java.util.UUID?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val projectStatus =
            if (status != null) {
                try {
                    ProjectStatus.valueOf(status.uppercase(Locale.ROOT))
                } catch (e: IllegalArgumentException) {
                    return ResponseEntity.badRequest().body(mapOf("error" to "Invalid status '$status'"))
                }
            } else {
                null
            }
        return ResponseEntity.ok(projectService.listProjects(orgId, projectStatus, customerId).map { ProjectResponse.from(it) })
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('projects:read')")
    fun getProject(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(ProjectResponse.from(projectService.getProject(id, orgId)))
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('projects:write')")
    fun updateProject(
        @PathVariable id: java.util.UUID,
        @Valid @RequestBody request: UpdateProjectRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(ProjectResponse.from(projectService.updateProject(id, request, orgId)))
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('projects:write')")
    fun activateProject(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(ProjectResponse.from(projectService.activateProject(id, orgId)))
    }

    @PostMapping("/{id}/hold")
    @PreAuthorize("hasAuthority('projects:write')")
    fun holdProject(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(ProjectResponse.from(projectService.holdProject(id, orgId)))
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAuthority('projects:write')")
    fun closeProject(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(ProjectResponse.from(projectService.closeProject(id, orgId)))
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('projects:write')")
    fun cancelProject(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(ProjectResponse.from(projectService.cancelProject(id, orgId)))
    }
}
