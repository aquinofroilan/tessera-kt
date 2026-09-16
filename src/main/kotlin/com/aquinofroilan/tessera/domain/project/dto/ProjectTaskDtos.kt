package com.aquinofroilan.tessera.domain.project.dto

import com.aquinofroilan.tessera.domain.project.model.ProjectTask
import com.aquinofroilan.tessera.domain.project.model.ProjectTaskDependency
import com.aquinofroilan.tessera.domain.project.model.TaskDependencyType
import com.aquinofroilan.tessera.domain.project.model.TaskStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDate

data class CreateProjectTaskRequest(
    @field:NotBlank(message = "Name is required")
    val name: String,
    val description: String? = null,
    val parentTaskId: java.util.UUID? = null,
    val assigneeEmployeeId: java.util.UUID? = null,
    val estimatedHours: BigDecimal? = null,
    val plannedStartDate: LocalDate? = null,
    val plannedFinishDate: LocalDate? = null,
)

data class UpdateProjectTaskRequest(
    val name: String? = null,
    val description: String? = null,
    val assigneeEmployeeId: java.util.UUID? = null,
    val estimatedHours: BigDecimal? = null,
    val plannedStartDate: LocalDate? = null,
    val plannedFinishDate: LocalDate? = null,
    val actualStartDate: LocalDate? = null,
    val actualFinishDate: LocalDate? = null,
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
    val plannedStartDate: LocalDate?,
    val plannedFinishDate: LocalDate?,
    val actualStartDate: LocalDate?,
    val actualFinishDate: LocalDate?,
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
                plannedStartDate = task.plannedStartDate,
                plannedFinishDate = task.plannedFinishDate,
                actualStartDate = task.actualStartDate,
                actualFinishDate = task.actualFinishDate,
                status = task.status,
                organizationId = task.organizationId,
                createdAt = task.createdAt?.toString(),
                updatedAt = task.updatedAt?.toString(),
            )
    }
}

data class CreateTaskDependencyRequest(
    @field:NotNull(message = "Predecessor task is required")
    val predecessorTaskId: java.util.UUID?,
    @field:NotNull(message = "Dependency type is required")
    val dependencyType: TaskDependencyType?,
)

data class TaskDependencyResponse(
    val id: java.util.UUID,
    val predecessorTaskId: java.util.UUID,
    val successorTaskId: java.util.UUID,
    val dependencyType: TaskDependencyType,
) {
    companion object {
        fun from(dependency: ProjectTaskDependency) =
            TaskDependencyResponse(
                id = dependency.id,
                predecessorTaskId = dependency.predecessorTaskId,
                successorTaskId = dependency.successorTaskId,
                dependencyType = dependency.dependencyType,
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
