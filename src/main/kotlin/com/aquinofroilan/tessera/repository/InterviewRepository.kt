package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.Interview
import com.aquinofroilan.tessera.model.InterviewStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface InterviewRepository : JpaRepository<Interview, String> {
    fun findByOrganizationIdAndApplicationIdOrderByScheduledAtAsc(
        organizationId: String,
        applicationId: String,
    ): List<Interview>

    fun findByOrganizationIdAndStatus(
        organizationId: String,
        status: InterviewStatus,
    ): List<Interview>
}
