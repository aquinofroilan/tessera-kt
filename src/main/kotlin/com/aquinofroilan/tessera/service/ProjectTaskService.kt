package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateProjectTaskRequest
import com.aquinofroilan.tessera.dto.ProjectTaskTreeNode
import com.aquinofroilan.tessera.dto.UpdateProjectTaskRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.ProjectTask
import com.aquinofroilan.tessera.repository.ProjectTaskRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProjectTaskService(
    private val projectTaskRepository: ProjectTaskRepository,
    private val projectService: ProjectService,
    private val employeeService: EmployeeService,
) {
    @Transactional
    fun createTask(
        projectId: String,
        request: CreateProjectTaskRequest,
        organizationId: String,
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
                organizationId = organizationId,
            ),
        )
    }

    fun getTask(
        projectId: String,
        taskId: String,
        organizationId: String,
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
        projectId: String,
        organizationId: String,
    ): List<ProjectTask> {
        projectService.getProject(projectId, organizationId)
        return projectTaskRepository.findByOrganizationIdAndProjectId(organizationId, projectId)
    }

    /** Builds the work-breakdown tree for a project: root tasks with nested children. */
    fun getTaskTree(
        projectId: String,
        organizationId: String,
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
        projectId: String,
        taskId: String,
        request: UpdateProjectTaskRequest,
        organizationId: String,
    ): ProjectTask {
        val task = getTask(projectId, taskId, organizationId)
        request.assigneeEmployeeId?.let { employeeService.getEmployee(it, organizationId) }
        task.apply {
            name = request.name?.trim() ?: task.name
            description = request.description ?: task.description
            assigneeEmployeeId = request.assigneeEmployeeId ?: task.assigneeEmployeeId
            estimatedHours = request.estimatedHours ?: task.estimatedHours
            status = request.status ?: task.status
        }
        return projectTaskRepository.save(task)
    }

    /**
     * Sets or clears a task's parent. A null parent promotes it to a root.
     * Rejects self-parenting, cross-project parents, and descendant cycles.
     */
    @Transactional
    fun setParent(
        projectId: String,
        taskId: String,
        parentTaskId: String?,
        organizationId: String,
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

    private fun requireTaskInProject(
        taskId: String,
        projectId: String,
        organizationId: String,
    ) {
        getTask(projectId, taskId, organizationId)
    }

    private fun descendantIds(
        taskId: String,
        projectId: String,
        organizationId: String,
    ): Set<String> {
        val childrenByParent =
            projectTaskRepository.findByOrganizationIdAndProjectId(organizationId, projectId).groupBy { it.parentTaskId }
        val descendants = mutableSetOf<String>()
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
