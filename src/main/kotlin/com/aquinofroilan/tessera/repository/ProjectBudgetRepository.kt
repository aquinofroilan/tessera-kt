package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.ProjectBudget
import com.aquinofroilan.tessera.model.ProjectCostCategory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface ProjectBudgetRepository : JpaRepository<ProjectBudget, java.util.UUID> {
    fun findByOrganizationIdAndProjectId(
        organizationId: java.util.UUID,
        projectId: java.util.UUID,
    ): List<ProjectBudget>

    fun findByOrganizationIdAndProjectIdAndCategory(
        organizationId: java.util.UUID,
        projectId: java.util.UUID,
        category: ProjectCostCategory,
    ): Optional<ProjectBudget>
}
