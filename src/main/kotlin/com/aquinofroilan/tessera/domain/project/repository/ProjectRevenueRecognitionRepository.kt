package com.aquinofroilan.tessera.domain.project.repository

import com.aquinofroilan.tessera.domain.project.model.ProjectRevenueRecognition
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ProjectRevenueRecognitionRepository : JpaRepository<ProjectRevenueRecognition, UUID> {
    fun findByOrganizationIdAndProjectId(
        organizationId: UUID,
        projectId: UUID,
    ): List<ProjectRevenueRecognition>
}
