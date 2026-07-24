package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateProjectRequest
import com.aquinofroilan.tessera.dto.UpdateProjectRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.Project
import com.aquinofroilan.tessera.model.ProjectBillingType
import com.aquinofroilan.tessera.model.ProjectStatus
import com.aquinofroilan.tessera.repository.ProjectRepository
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProjectService(
    private val projectRepository: ProjectRepository,
    private val customerService: CustomerService,
    private val employeeService: EmployeeService,
) {
    @Transactional
    fun createProject(
        request: CreateProjectRequest,
        organizationId: java.util.UUID,
    ): Project {
        val startDate = request.startDate ?: throw BusinessRuleException("Start date is required")
        if (request.endDate != null && request.endDate.isBefore(startDate)) {
            throw BusinessRuleException("End date cannot be before the start date")
        }
        request.customerId?.let { customerService.getCustomer(it, organizationId) }
        request.managerEmployeeId?.let { employeeService.getEmployee(it, organizationId) }

        return saveWithRetry(organizationId) { number ->
            Project(
                projectNumber = number,
                name = request.name.trim(),
                description = request.description,
                customerId = request.customerId,
                managerEmployeeId = request.managerEmployeeId,
                startDate = startDate,
                endDate = request.endDate,
                billingType = request.billingType ?: ProjectBillingType.TIME_AND_MATERIALS,
                organizationId = organizationId,
            )
        }
    }

    fun getProject(
        id: java.util.UUID,
        organizationId: java.util.UUID,
    ): Project {
        val project =
            projectRepository.findById(id).orElseThrow {
                ResourceNotFoundException("Project not found")
            }
        if (project.organizationId != organizationId) {
            throw ResourceNotFoundException("Project not found")
        }
        return project
    }

    fun listProjects(
        organizationId: java.util.UUID,
        status: ProjectStatus? = null,
        customerId: java.util.UUID? = null,
    ): List<Project> =
        when {
            status != null -> projectRepository.findByOrganizationIdAndStatus(organizationId, status)
            customerId != null -> projectRepository.findByOrganizationIdAndCustomerId(organizationId, customerId)
            else -> projectRepository.findByOrganizationId(organizationId)
        }

    @Transactional
    fun updateProject(
        id: java.util.UUID,
        request: UpdateProjectRequest,
        organizationId: java.util.UUID,
    ): Project {
        val project = getProject(id, organizationId)
        request.customerId?.let { customerService.getCustomer(it, organizationId) }
        request.managerEmployeeId?.let { employeeService.getEmployee(it, organizationId) }
        val endDate = request.endDate ?: project.endDate
        if (endDate != null && endDate.isBefore(project.startDate)) {
            throw BusinessRuleException("End date cannot be before the start date")
        }
        project.apply {
            name = request.name?.trim() ?: project.name
            description = request.description ?: project.description
            customerId = request.customerId ?: project.customerId
            managerEmployeeId = request.managerEmployeeId ?: project.managerEmployeeId
            this.endDate = endDate
            billingType = request.billingType ?: project.billingType
        }
        return projectRepository.save(project)
    }

    @Transactional
    fun activateProject(
        id: java.util.UUID,
        organizationId: java.util.UUID,
    ): Project {
        val project = getProject(id, organizationId)
        if (project.status != ProjectStatus.PLANNED && project.status != ProjectStatus.ON_HOLD) {
            throw BusinessRuleException("Only planned or on-hold projects can be activated")
        }
        project.status = ProjectStatus.ACTIVE
        return projectRepository.save(project)
    }

    @Transactional
    fun holdProject(
        id: java.util.UUID,
        organizationId: java.util.UUID,
    ): Project {
        val project = getProject(id, organizationId)
        if (project.status != ProjectStatus.ACTIVE) {
            throw BusinessRuleException("Only active projects can be put on hold")
        }
        project.status = ProjectStatus.ON_HOLD
        return projectRepository.save(project)
    }

    @Transactional
    fun closeProject(
        id: java.util.UUID,
        organizationId: java.util.UUID,
    ): Project {
        val project = getProject(id, organizationId)
        if (project.status != ProjectStatus.ACTIVE && project.status != ProjectStatus.ON_HOLD) {
            throw BusinessRuleException("Only active or on-hold projects can be closed")
        }
        project.status = ProjectStatus.CLOSED
        return projectRepository.save(project)
    }

    @Transactional
    fun cancelProject(
        id: java.util.UUID,
        organizationId: java.util.UUID,
    ): Project {
        val project = getProject(id, organizationId)
        if (project.status == ProjectStatus.CLOSED || project.status == ProjectStatus.CANCELLED) {
            throw BusinessRuleException("Project is already ${project.status.name.lowercase()}")
        }
        project.status = ProjectStatus.CANCELLED
        return projectRepository.save(project)
    }

    private fun saveWithRetry(
        organizationId: java.util.UUID,
        maxRetries: Int = 3,
        build: (String) -> Project,
    ): Project {
        repeat(maxRetries) { attempt ->
            val count = projectRepository.countByOrganizationId(organizationId)
            val number = "PRJ-${(count + 1).toString().padStart(4, '0')}"
            try {
                return projectRepository.save(build(number))
            } catch (e: DuplicateKeyException) {
                if (attempt == maxRetries - 1) {
                    throw IllegalStateException("Failed to generate unique project number: $number", e)
                }
            }
        }
        throw IllegalStateException("Failed to generate unique project number")
    }
}
