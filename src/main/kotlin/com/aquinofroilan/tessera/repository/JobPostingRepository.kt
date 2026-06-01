package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.JobPosting
import com.aquinofroilan.tessera.model.JobPostingStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface JobPostingRepository : JpaRepository<JobPosting, String> {
    fun findByOrganizationId(organizationId: String): List<JobPosting>

    fun findByOrganizationIdAndStatus(
        organizationId: String,
        status: JobPostingStatus,
    ): List<JobPosting>
}
