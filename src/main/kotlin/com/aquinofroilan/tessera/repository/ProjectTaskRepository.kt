package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.ProjectTask
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ProjectTaskRepository : JpaRepository<ProjectTask, java.util.UUID> {
    fun findByOrganizationIdAndProjectId(
        organizationId: java.util.UUID,
        projectId: java.util.UUID,
    ): List<ProjectTask>
}
