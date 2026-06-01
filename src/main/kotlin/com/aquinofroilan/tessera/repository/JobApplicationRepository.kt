package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.JobApplication
import com.aquinofroilan.tessera.model.JobApplicationStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface JobApplicationRepository : JpaRepository<JobApplication, String> {
    fun findByOrganizationId(organizationId: String): List<JobApplication>

    fun findByOrganizationIdAndJobPostingId(
        organizationId: String,
        jobPostingId: String,
    ): List<JobApplication>

    fun findByOrganizationIdAndStatus(
        organizationId: String,
        status: JobApplicationStatus,
    ): List<JobApplication>
}
