package com.aquinofroilan.tessera.domain.reporting.repository

import com.aquinofroilan.tessera.domain.reporting.model.ScheduledReport
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ScheduledReportRepository : JpaRepository<ScheduledReport, UUID> {
    fun findByOrganizationId(
        organizationId: UUID,
        pageable: Pageable,
    ): Page<ScheduledReport>
}
