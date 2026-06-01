package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.ProjectBudget
import com.aquinofroilan.tessera.model.ProjectCostCategory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface ProjectBudgetRepository : JpaRepository<ProjectBudget, String> {
    fun findByOrganizationIdAndProjectId(
        organizationId: String,
        projectId: String,
    ): List<ProjectBudget>

    fun findByOrganizationIdAndProjectIdAndCategory(
        organizationId: String,
        projectId: String,
        category: ProjectCostCategory,
    ): Optional<ProjectBudget>
}
