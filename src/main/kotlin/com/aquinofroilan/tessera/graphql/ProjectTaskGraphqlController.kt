package com.aquinofroilan.tessera.graphql

import com.aquinofroilan.tessera.domain.project.controller.ProjectTaskController
import com.aquinofroilan.tessera.domain.project.dto.CreateProjectTaskRequest
import com.aquinofroilan.tessera.domain.project.dto.SetTaskParentRequest
import com.aquinofroilan.tessera.domain.project.dto.UpdateProjectTaskRequest
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

/**
 * GraphQL bridge for project tasks (work breakdown structure), delegating to the
 * REST controller via the shared JSON-scalar pass-through. Authorization mirrors
 * the REST endpoints (`projects:read`/`projects:write`).
 */
@Controller
class ProjectTaskGraphqlController(
    private val projectTaskController: ProjectTaskController,
    private val support: GraphqlBridgeSupport,
) {
    @QueryMapping
    @PreAuthorize("hasAuthority('projects:read')")
    fun projectTasks(
        @Argument projectId: java.util.UUID,
    ): Any = support.unwrap(projectTaskController.listTasks(projectId))

    @QueryMapping
    @PreAuthorize("hasAuthority('projects:read')")
    fun projectTaskTree(
        @Argument projectId: java.util.UUID,
    ): Any = support.unwrap(projectTaskController.taskTree(projectId))

    @QueryMapping
    @PreAuthorize("hasAuthority('projects:read')")
    fun projectTask(
        @Argument projectId: java.util.UUID,
        @Argument taskId: java.util.UUID,
    ): Any = support.unwrap(projectTaskController.getTask(projectId, taskId))

    @MutationMapping
    @PreAuthorize("hasAuthority('projects:write')")
    fun createProjectTask(
        @Argument projectId: java.util.UUID,
        @Argument input: Any,
    ): Any = support.unwrap(projectTaskController.createTask(projectId, support.toRequest<CreateProjectTaskRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('projects:write')")
    fun updateProjectTask(
        @Argument projectId: java.util.UUID,
        @Argument taskId: java.util.UUID,
        @Argument input: Any,
    ): Any = support.unwrap(projectTaskController.updateTask(projectId, taskId, support.toRequest<UpdateProjectTaskRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('projects:write')")
    fun setProjectTaskParent(
        @Argument projectId: java.util.UUID,
        @Argument taskId: java.util.UUID,
        @Argument input: Any?,
    ): Any {
        val request = input?.let { support.toRequest<SetTaskParentRequest>(it) } ?: SetTaskParentRequest()
        return support.unwrap(projectTaskController.setParent(projectId, taskId, request))
    }
}
