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
        organizationId: String,
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
        id: String,
        organizationId: String,
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
        organizationId: String,
        status: ProjectStatus? = null,
        customerId: String? = null,
    ): List<Project> =
        when {
            status != null -> projectRepository.findByOrganizationIdAndStatus(organizationId, status)
            customerId != null -> projectRepository.findByOrganizationIdAndCustomerId(organizationId, customerId)
            else -> projectRepository.findByOrganizationId(organizationId)
        }

    @Transactional
    fun updateProject(
        id: String,
        request: UpdateProjectRequest,
        organizationId: String,
    ): Project {
        val project = getProject(id, organizationId)
        request.customerId?.let { customerService.getCustomer(it, organizationId) }
        request.managerEmployeeId?.let { employeeService.getEmployee(it, organizationId) }
        val endDate = request.endDate ?: project.endDate
        if (endDate != null && endDate.isBefore(project.startDate)) {
            throw BusinessRuleException("End date cannot be before the start date")
        }
        return projectRepository.save(
            project.copy(
                name = request.name?.trim() ?: project.name,
                description = request.description ?: project.description,
                customerId = request.customerId ?: project.customerId,
                managerEmployeeId = request.managerEmployeeId ?: project.managerEmployeeId,
                endDate = endDate,
                billingType = request.billingType ?: project.billingType,
            ),
        )
    }

    @Transactional
    fun activateProject(
        id: String,
        organizationId: String,
    ): Project {
        val project = getProject(id, organizationId)
        if (project.status != ProjectStatus.PLANNED && project.status != ProjectStatus.ON_HOLD) {
            throw BusinessRuleException("Only planned or on-hold projects can be activated")
        }
        return projectRepository.save(project.copy(status = ProjectStatus.ACTIVE))
    }

    @Transactional
    fun holdProject(
        id: String,
        organizationId: String,
    ): Project {
        val project = getProject(id, organizationId)
        if (project.status != ProjectStatus.ACTIVE) {
            throw BusinessRuleException("Only active projects can be put on hold")
        }
        return projectRepository.save(project.copy(status = ProjectStatus.ON_HOLD))
    }

    @Transactional
    fun closeProject(
        id: String,
        organizationId: String,
    ): Project {
        val project = getProject(id, organizationId)
        if (project.status != ProjectStatus.ACTIVE && project.status != ProjectStatus.ON_HOLD) {
            throw BusinessRuleException("Only active or on-hold projects can be closed")
        }
        return projectRepository.save(project.copy(status = ProjectStatus.CLOSED))
    }

    @Transactional
    fun cancelProject(
        id: String,
        organizationId: String,
    ): Project {
        val project = getProject(id, organizationId)
        if (project.status == ProjectStatus.CLOSED || project.status == ProjectStatus.CANCELLED) {
            throw BusinessRuleException("Project is already ${project.status.name.lowercase()}")
        }
        return projectRepository.save(project.copy(status = ProjectStatus.CANCELLED))
    }

    private fun saveWithRetry(
        organizationId: String,
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
