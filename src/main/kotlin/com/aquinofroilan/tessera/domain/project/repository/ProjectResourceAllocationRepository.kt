package com.aquinofroilan.tessera.domain.project.repository

import com.aquinofroilan.tessera.domain.project.model.ProjectResourceAllocation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ProjectResourceAllocationRepository : JpaRepository<ProjectResourceAllocation, UUID> {
    fun findByOrganizationIdAndProjectId(
        organizationId: UUID,
        projectId: UUID,
    ): List<ProjectResourceAllocation>

    fun findByOrganizationIdAndEmployeeId(
        organizationId: UUID,
        employeeId: UUID,
    ): List<ProjectResourceAllocation>
}
