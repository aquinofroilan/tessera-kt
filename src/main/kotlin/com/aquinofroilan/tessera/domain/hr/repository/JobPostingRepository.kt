package com.aquinofroilan.tessera.domain.hr.repository

import com.aquinofroilan.tessera.domain.hr.model.JobPosting
import com.aquinofroilan.tessera.domain.hr.model.JobPostingStatus
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
