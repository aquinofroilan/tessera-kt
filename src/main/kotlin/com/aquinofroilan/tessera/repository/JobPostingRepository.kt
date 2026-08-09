package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.JobPosting
import com.aquinofroilan.tessera.model.JobPostingStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface JobPostingRepository : JpaRepository<JobPosting, UUID> {
    fun findByOrganizationId(organizationId: UUID): List<JobPosting>

    fun findByOrganizationIdAndStatus(
        organizationId: UUID,
        status: JobPostingStatus,
    ): List<JobPosting>
}
