package com.aquinofroilan.tessera.domain.hr.repository

import com.aquinofroilan.tessera.domain.hr.model.JobApplication
import com.aquinofroilan.tessera.domain.hr.model.JobApplicationStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface JobApplicationRepository : JpaRepository<JobApplication, UUID> {
    fun findByOrganizationId(organizationId: UUID): List<JobApplication>

    fun findByOrganizationIdAndJobPostingId(
        organizationId: UUID,
        jobPostingId: UUID,
    ): List<JobApplication>

    fun findByOrganizationIdAndStatus(
        organizationId: UUID,
        status: JobApplicationStatus,
    ): List<JobApplication>
}
