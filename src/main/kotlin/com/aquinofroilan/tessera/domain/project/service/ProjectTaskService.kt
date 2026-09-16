package com.aquinofroilan.tessera.domain.project.service

import com.aquinofroilan.tessera.domain.hr.service.EmployeeService
import com.aquinofroilan.tessera.domain.project.dto.CreateProjectTaskRequest
import com.aquinofroilan.tessera.domain.project.dto.CreateTaskDependencyRequest
import com.aquinofroilan.tessera.domain.project.dto.ProjectTaskTreeNode
import com.aquinofroilan.tessera.domain.project.dto.UpdateProjectTaskRequest
import com.aquinofroilan.tessera.domain.project.model.ProjectTask
import com.aquinofroilan.tessera.domain.project.model.ProjectTaskDependency
import com.aquinofroilan.tessera.domain.project.model.TaskDependencyType
import com.aquinofroilan.tessera.domain.project.repository.ProjectTaskDependencyRepository
import com.aquinofroilan.tessera.domain.project.repository.ProjectTaskRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ProjectTaskService(
    private val projectTaskRepository: ProjectTaskRepository,
    private val projectTaskDependencyRepository: ProjectTaskDependencyRepository,
    private val projectService: ProjectService,
    private val employeeService: EmployeeService,
) {
    @Transactional
    fun createTask(
        projectId: java.util.UUID,
        request: CreateProjectTaskRequest,
        organizationId: java.util.UUID,
    ): ProjectTask {
        projectService.getProject(projectId, organizationId)
        request.assigneeEmployeeId?.let { employeeService.getEmployee(it, organizationId) }
        request.parentTaskId?.let { requireTaskInProject(it, projectId, organizationId) }
        return projectTaskRepository.save(
            ProjectTask(
                projectId = projectId,
                parentTaskId = request.parentTaskId,
                name = request.name.trim(),
                description = request.description,
                assigneeEmployeeId = request.assigneeEmployeeId,
                estimatedHours = request.estimatedHours,
                plannedStartDate = request.plannedStartDate,
                plannedFinishDate = request.plannedFinishDate,
                organizationId = organizationId,
            ),
        )
    }

    fun getTask(
        projectId: java.util.UUID,
        taskId: java.util.UUID,
        organizationId: java.util.UUID,
    ): ProjectTask {
        val task =
            projectTaskRepository.findById(taskId).orElseThrow {
                ResourceNotFoundException("Task not found")
            }
        if (task.organizationId != organizationId || task.projectId != projectId) {
            throw ResourceNotFoundException("Task not found")
        }
        return task
    }

    fun listTasks(
        projectId: java.util.UUID,
        organizationId: java.util.UUID,
    ): List<ProjectTask> {
        projectService.getProject(projectId, organizationId)
        return projectTaskRepository.findByOrganizationIdAndProjectId(organizationId, projectId)
    }

    fun getTaskTree(
        projectId: java.util.UUID,
        organizationId: java.util.UUID,
    ): List<ProjectTaskTreeNode> {
        val all = listTasks(projectId, organizationId)
        val childrenByParent = all.groupBy { it.parentTaskId }

        fun build(task: ProjectTask): ProjectTaskTreeNode =
            ProjectTaskTreeNode.from(
                task,
                childrenByParent[task.id].orEmpty().sortedBy { it.name }.map(::build),
            )
        return childrenByParent[null].orEmpty().sortedBy { it.name }.map(::build)
    }

    @Transactional
    fun updateTask(
        projectId: java.util.UUID,
        taskId: java.util.UUID,
        request: UpdateProjectTaskRequest,
        organizationId: java.util.UUID,
    ): ProjectTask {
        val task = getTask(projectId, taskId, organizationId)
        request.assigneeEmployeeId?.let { employeeService.getEmployee(it, organizationId) }
        task.apply {
            name = request.name?.trim() ?: task.name
            description = request.description ?: task.description
            assigneeEmployeeId = request.assigneeEmployeeId ?: task.assigneeEmployeeId
            estimatedHours = request.estimatedHours ?: task.estimatedHours
            plannedStartDate = request.plannedStartDate ?: task.plannedStartDate
            plannedFinishDate = request.plannedFinishDate ?: task.plannedFinishDate
            actualStartDate = request.actualStartDate ?: task.actualStartDate
            actualFinishDate = request.actualFinishDate ?: task.actualFinishDate
            status = request.status ?: task.status
        }
        return projectTaskRepository.save(task)
    }

    @Transactional
    fun setParent(
        projectId: java.util.UUID,
        taskId: java.util.UUID,
        parentTaskId: java.util.UUID?,
        organizationId: java.util.UUID,
    ): ProjectTask {
        val task = getTask(projectId, taskId, organizationId)
        if (parentTaskId == null) {
            task.parentTaskId = null
            return projectTaskRepository.save(task)
        }
        if (parentTaskId == taskId) {
            throw BusinessRuleException("A task cannot be its own parent")
        }
        requireTaskInProject(parentTaskId, projectId, organizationId)
        if (descendantIds(taskId, projectId, organizationId).contains(parentTaskId)) {
            throw BusinessRuleException("Cannot move a task under one of its own descendants")
        }
        task.parentTaskId = parentTaskId
        return projectTaskRepository.save(task)
    }

    @Transactional
    fun addDependency(
        projectId: UUID,
        successorTaskId: UUID,
        request: CreateTaskDependencyRequest,
        organizationId: UUID,
    ): ProjectTaskDependency {
        val successor = getTask(projectId, successorTaskId, organizationId)
        val predId = request.predecessorTaskId ?: throw BusinessRuleException("Predecessor ID required")
        val depType = request.dependencyType ?: throw BusinessRuleException("Dependency type required")

        requireTaskInProject(predId, projectId, organizationId)
        if (predId == successorTaskId) {
            throw BusinessRuleException("A task cannot depend on itself")
        }

        val existing =
            projectTaskDependencyRepository.findByOrganizationIdAndPredecessorTaskIdAndSuccessorTaskId(
                organizationId,
                predId,
                successorTaskId,
            )
        if (existing != null) {
            existing.dependencyType = depType
            return projectTaskDependencyRepository.save(existing)
        }

        return projectTaskDependencyRepository.save(
            ProjectTaskDependency(
                organizationId = organizationId,
                predecessorTaskId = predId,
                successorTaskId = successorTaskId,
                dependencyType = depType,
            ),
        )
    }

    fun getPredecessors(
        projectId: UUID,
        taskId: UUID,
        organizationId: UUID,
    ): List<ProjectTaskDependency> {
        getTask(projectId, taskId, organizationId)
        return projectTaskDependencyRepository.findByOrganizationIdAndSuccessorTaskId(organizationId, taskId)
    }

    fun getSuccessors(
        projectId: UUID,
        taskId: UUID,
        organizationId: UUID,
    ): List<ProjectTaskDependency> {
        getTask(projectId, taskId, organizationId)
        return projectTaskDependencyRepository.findByOrganizationIdAndPredecessorTaskId(organizationId, taskId)
    }

    @Transactional
    fun recalculateSchedule(
        projectId: UUID,
        taskId: UUID,
        organizationId: UUID,
    ): ProjectTask {
        val task = getTask(projectId, taskId, organizationId)
        val predecessors = projectTaskDependencyRepository.findByOrganizationIdAndSuccessorTaskId(organizationId, taskId)

        var earliestStart = task.plannedStartDate
        var earliestFinish = task.plannedFinishDate

        for (dep in predecessors) {
            val pred = getTask(projectId, dep.predecessorTaskId, organizationId)
            when (dep.dependencyType) {
                TaskDependencyType.FINISH_TO_START -> {
                    pred.plannedFinishDate?.let {
                        if (earliestStart == null || it.isAfter(earliestStart)) {
                            earliestStart = it
                        }
                    }
                }
                TaskDependencyType.START_TO_START -> {
                    pred.plannedStartDate?.let {
                        if (earliestStart == null || it.isAfter(earliestStart)) {
                            earliestStart = it
                        }
                    }
                }
                TaskDependencyType.FINISH_TO_FINISH -> {
                    pred.plannedFinishDate?.let {
                        if (earliestFinish == null || it.isAfter(earliestFinish)) {
                            earliestFinish = it
                        }
                    }
                }
                TaskDependencyType.START_TO_FINISH -> {
                    pred.plannedStartDate?.let {
                        if (earliestFinish == null || it.isAfter(earliestFinish)) {
                            earliestFinish = it
                        }
                    }
                }
            }
        }

        task.plannedStartDate = earliestStart
        task.plannedFinishDate = earliestFinish
        return projectTaskRepository.save(task)
    }

    private fun requireTaskInProject(
        taskId: java.util.UUID,
        projectId: java.util.UUID,
        organizationId: java.util.UUID,
    ) {
        getTask(projectId, taskId, organizationId)
    }

    private fun descendantIds(
        taskId: java.util.UUID,
        projectId: java.util.UUID,
        organizationId: java.util.UUID,
    ): Set<java.util.UUID> {
        val childrenByParent =
            projectTaskRepository.findByOrganizationIdAndProjectId(organizationId, projectId).groupBy { it.parentTaskId }
        val descendants = mutableSetOf<java.util.UUID>()
        val queue = ArrayDeque(childrenByParent[taskId].orEmpty().map { it.id })
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (descendants.add(current)) {
                childrenByParent[current].orEmpty().forEach { queue.addLast(it.id) }
            }
        }
        return descendants
    }
}
