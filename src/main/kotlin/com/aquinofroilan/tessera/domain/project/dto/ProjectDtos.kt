package com.aquinofroilan.tessera.domain.project.dto

import com.aquinofroilan.tessera.domain.project.model.Project
import com.aquinofroilan.tessera.domain.project.model.ProjectBillingType
import com.aquinofroilan.tessera.domain.project.model.ProjectStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

data class CreateProjectRequest(
    @field:NotBlank(message = "Name is required")
    val name: String,
    val description: String? = null,
    val customerId: java.util.UUID? = null,
    val managerEmployeeId: java.util.UUID? = null,
    @field:NotNull(message = "Start date is required")
    val startDate: LocalDate?,
    val endDate: LocalDate? = null,
    val billingType: ProjectBillingType? = null,
)

data class UpdateProjectRequest(
    val name: String? = null,
    val description: String? = null,
    val customerId: java.util.UUID? = null,
    val managerEmployeeId: java.util.UUID? = null,
    val endDate: LocalDate? = null,
    val billingType: ProjectBillingType? = null,
)

data class ProjectResponse(
    val id: java.util.UUID,
    val projectNumber: String,
    val name: String,
    val description: String?,
    val customerId: java.util.UUID?,
    val managerEmployeeId: java.util.UUID?,
    val startDate: String,
    val endDate: String?,
    val status: ProjectStatus,
    val billingType: ProjectBillingType,
    val organizationId: java.util.UUID,
    val createdAt: String?,
    val updatedAt: String?,
) {
    companion object {
        fun from(project: Project) =
            ProjectResponse(
                id = project.id,
                projectNumber = project.projectNumber,
                name = project.name,
                description = project.description,
                customerId = project.customerId,
                managerEmployeeId = project.managerEmployeeId,
                startDate = project.startDate.toString(),
                endDate = project.endDate?.toString(),
                status = project.status,
                billingType = project.billingType,
                organizationId = project.organizationId,
                createdAt = project.createdAt?.toString(),
                updatedAt = project.updatedAt?.toString(),
            )
    }
}
