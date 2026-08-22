package com.aquinofroilan.tessera.graphql

import com.aquinofroilan.tessera.domain.project.controller.ProjectController
import com.aquinofroilan.tessera.domain.project.dto.CreateProjectRequest
import com.aquinofroilan.tessera.domain.project.dto.UpdateProjectRequest
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

/**
 * GraphQL bridge for the project routes, delegating to the REST controller via
 * the shared JSON-scalar pass-through. Authorization mirrors the REST endpoints
 * (`projects:read`/`projects:write`).
 */
@Controller
class ProjectGraphqlController(
    private val projectController: ProjectController,
    private val support: GraphqlBridgeSupport,
) {
    @QueryMapping
    @PreAuthorize("hasAuthority('projects:read')")
    fun projects(
        @Argument status: String?,
        @Argument customerId: java.util.UUID?,
    ): Any = support.unwrap(projectController.listProjects(status, customerId))

    @QueryMapping
    @PreAuthorize("hasAuthority('projects:read')")
    fun project(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(projectController.getProject(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('projects:write')")
    fun createProject(
        @Argument input: Any,
    ): Any = support.unwrap(projectController.createProject(support.toRequest<CreateProjectRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('projects:write')")
    fun updateProject(
        @Argument id: java.util.UUID,
        @Argument input: Any,
    ): Any = support.unwrap(projectController.updateProject(id, support.toRequest<UpdateProjectRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('projects:write')")
    fun activateProject(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(projectController.activateProject(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('projects:write')")
    fun holdProject(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(projectController.holdProject(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('projects:write')")
    fun closeProject(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(projectController.closeProject(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('projects:write')")
    fun cancelProject(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(projectController.cancelProject(id))
}
