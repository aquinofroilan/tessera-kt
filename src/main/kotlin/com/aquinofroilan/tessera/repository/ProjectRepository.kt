package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.Project
import com.aquinofroilan.tessera.model.ProjectStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ProjectRepository : JpaRepository<Project, String> {
    fun findByOrganizationId(organizationId: String): List<Project>

    fun findByOrganizationIdAndStatus(
        organizationId: String,
        status: ProjectStatus,
    ): List<Project>

    fun findByOrganizationIdAndCustomerId(
        organizationId: String,
        customerId: String,
    ): List<Project>

    fun countByOrganizationId(organizationId: String): Long
}
