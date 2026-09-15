package com.aquinofroilan.tessera.domain.project.dto

import com.aquinofroilan.tessera.domain.project.model.ProjectResourceAllocation
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class CreateResourceAllocationRequest(
    @field:NotNull(message = "Employee ID is required")
    val employeeId: UUID?,
    @field:NotNull(message = "Start date is required")
    val startDate: LocalDate?,
    @field:NotNull(message = "End date is required")
    val endDate: LocalDate?,
    @field:NotNull(message = "Allocated hours is required")
    val allocatedHours: BigDecimal?,
)

data class UpdateResourceAllocationRequest(
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val allocatedHours: BigDecimal? = null,
)

data class ResourceAllocationResponse(
    val id: UUID,
    val projectId: UUID,
    val employeeId: UUID,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val allocatedHours: BigDecimal,
    val organizationId: UUID,
    val createdAt: String?,
    val updatedAt: String?,
) {
    companion object {
        fun from(allocation: ProjectResourceAllocation) =
            ResourceAllocationResponse(
                id = allocation.id,
                projectId = allocation.projectId,
                employeeId = allocation.employeeId,
                startDate = allocation.startDate,
                endDate = allocation.endDate,
                allocatedHours = allocation.allocatedHours,
                organizationId = allocation.organizationId,
                createdAt = allocation.createdAt?.toString(),
                updatedAt = allocation.updatedAt?.toString(),
            )
    }
}

data class ResourceUtilizationReport(
    val employeeId: UUID,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val allocatedHours: BigDecimal,
    val actualHours: BigDecimal,
    val utilizationPercentage: BigDecimal,
)
