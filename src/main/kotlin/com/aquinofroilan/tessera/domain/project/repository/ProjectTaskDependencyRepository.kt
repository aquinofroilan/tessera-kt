package com.aquinofroilan.tessera.domain.project.repository

import com.aquinofroilan.tessera.domain.project.model.ProjectTaskDependency
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ProjectTaskDependencyRepository : JpaRepository<ProjectTaskDependency, UUID> {
    fun findByOrganizationIdAndSuccessorTaskId(
        organizationId: UUID,
        successorTaskId: UUID,
    ): List<ProjectTaskDependency>

    fun findByOrganizationIdAndPredecessorTaskId(
        organizationId: UUID,
        predecessorTaskId: UUID,
    ): List<ProjectTaskDependency>

    fun findByOrganizationIdAndPredecessorTaskIdAndSuccessorTaskId(
        organizationId: UUID,
        predecessorTaskId: UUID,
        successorTaskId: UUID,
    ): ProjectTaskDependency?
}
