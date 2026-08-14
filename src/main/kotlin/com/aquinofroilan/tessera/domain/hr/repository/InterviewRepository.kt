package com.aquinofroilan.tessera.domain.hr.repository

import com.aquinofroilan.tessera.domain.hr.model.Interview
import com.aquinofroilan.tessera.domain.hr.model.InterviewStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface InterviewRepository : JpaRepository<Interview, UUID> {
    fun findByOrganizationIdAndApplicationIdOrderByScheduledAtAsc(
        organizationId: UUID,
        applicationId: UUID,
    ): List<Interview>

    fun findByOrganizationIdAndStatus(
        organizationId: UUID,
        status: InterviewStatus,
    ): List<Interview>
}
