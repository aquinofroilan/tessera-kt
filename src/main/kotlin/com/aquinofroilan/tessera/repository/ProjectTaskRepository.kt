package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.ProjectTask
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ProjectTaskRepository : JpaRepository<ProjectTask, String> {
    fun findByOrganizationIdAndProjectId(
        organizationId: String,
        projectId: String,
    ): List<ProjectTask>
}
