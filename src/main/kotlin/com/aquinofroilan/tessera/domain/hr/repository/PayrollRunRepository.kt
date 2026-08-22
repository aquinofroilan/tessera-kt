package com.aquinofroilan.tessera.domain.hr.repository

import com.aquinofroilan.tessera.domain.hr.model.PayrollRun
import com.aquinofroilan.tessera.domain.hr.model.PayrollRunStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PayrollRunRepository : JpaRepository<PayrollRun, java.util.UUID> {
    fun findByOrganizationId(organizationId: java.util.UUID): List<PayrollRun>

    fun findByOrganizationIdAndStatus(
        organizationId: java.util.UUID,
        status: PayrollRunStatus,
    ): List<PayrollRun>

    fun countByOrganizationId(organizationId: java.util.UUID): Long
}
