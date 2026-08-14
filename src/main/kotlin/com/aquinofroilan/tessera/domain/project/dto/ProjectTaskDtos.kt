package com.aquinofroilan.tessera.domain.project.dto

import com.aquinofroilan.tessera.domain.project.model.ProjectTask
import com.aquinofroilan.tessera.domain.project.model.TaskStatus
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal

data class CreateProjectTaskRequest(
    @field:NotBlank(message = "Name is required")
    val name: String,
    val description: String? = null,
    val parentTaskId: java.util.UUID? = null,
    val assigneeEmployeeId: java.util.UUID? = null,
    val estimatedHours: BigDecimal? = null,
)

data class UpdateProjectTaskRequest(
    val name: String? = null,
    val description: String? = null,
    val assigneeEmployeeId: java.util.UUID? = null,
    val estimatedHours: BigDecimal? = null,
    val status: TaskStatus? = null,
)

/** Sets or clears a task's parent within the same project. */
data class SetTaskParentRequest(
    val parentTaskId: java.util.UUID? = null,
)

data class ProjectTaskResponse(
    val id: java.util.UUID,
    val projectId: java.util.UUID,
    val parentTaskId: java.util.UUID?,
    val name: String,
    val description: String?,
    val assigneeEmployeeId: java.util.UUID?,
    val estimatedHours: BigDecimal?,
    val status: TaskStatus,
    val organizationId: java.util.UUID,
    val createdAt: String?,
    val updatedAt: String?,
) {
    companion object {
        fun from(task: ProjectTask) =
            ProjectTaskResponse(
                id = task.id,
                projectId = task.projectId,
                parentTaskId = task.parentTaskId,
                name = task.name,
                description = task.description,
                assigneeEmployeeId = task.assigneeEmployeeId,
                estimatedHours = task.estimatedHours,
                status = task.status,
                organizationId = task.organizationId,
                createdAt = task.createdAt?.toString(),
                updatedAt = task.updatedAt?.toString(),
            )
    }
}

/** A node in the project work-breakdown tree: the task plus its nested children. */
data class ProjectTaskTreeNode(
    val id: java.util.UUID,
    val name: String,
    val status: TaskStatus,
    val assigneeEmployeeId: java.util.UUID?,
    val children: List<ProjectTaskTreeNode>,
) {
    companion object {
        fun from(
            task: ProjectTask,
            children: List<ProjectTaskTreeNode>,
        ) = ProjectTaskTreeNode(
            id = task.id,
            name = task.name,
            status = task.status,
            assigneeEmployeeId = task.assigneeEmployeeId,
            children = children,
        )
    }
}
