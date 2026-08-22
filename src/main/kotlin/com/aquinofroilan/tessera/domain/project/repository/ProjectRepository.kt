package com.aquinofroilan.tessera.domain.project.repository

import com.aquinofroilan.tessera.domain.project.model.Project
import com.aquinofroilan.tessera.domain.project.model.ProjectStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ProjectRepository : JpaRepository<Project, java.util.UUID> {
    fun findByOrganizationId(organizationId: java.util.UUID): List<Project>

    fun findByOrganizationIdAndStatus(
        organizationId: java.util.UUID,
        status: ProjectStatus,
    ): List<Project>

    fun findByOrganizationIdAndCustomerId(
        organizationId: java.util.UUID,
        customerId: java.util.UUID,
    ): List<Project>

    fun countByOrganizationId(organizationId: java.util.UUID): Long
}
