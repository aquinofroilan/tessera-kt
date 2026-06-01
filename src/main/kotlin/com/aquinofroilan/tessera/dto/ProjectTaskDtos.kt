package com.aquinofroilan.tessera.dto

import com.aquinofroilan.tessera.model.ProjectTask
import com.aquinofroilan.tessera.model.TaskStatus
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal

data class CreateProjectTaskRequest(
    @field:NotBlank(message = "Name is required")
    val name: String,
    val description: String? = null,
    val parentTaskId: String? = null,
    val assigneeEmployeeId: String? = null,
    val estimatedHours: BigDecimal? = null,
)

data class UpdateProjectTaskRequest(
    val name: String? = null,
    val description: String? = null,
    val assigneeEmployeeId: String? = null,
    val estimatedHours: BigDecimal? = null,
    val status: TaskStatus? = null,
)

/** Sets or clears a task's parent within the same project. */
data class SetTaskParentRequest(
    val parentTaskId: String? = null,
)

data class ProjectTaskResponse(
    val id: String,
    val projectId: String,
    val parentTaskId: String?,
    val name: String,
    val description: String?,
    val assigneeEmployeeId: String?,
    val estimatedHours: BigDecimal?,
    val status: TaskStatus,
    val organizationId: String,
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
    val id: String,
    val name: String,
    val status: TaskStatus,
    val assigneeEmployeeId: String?,
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
