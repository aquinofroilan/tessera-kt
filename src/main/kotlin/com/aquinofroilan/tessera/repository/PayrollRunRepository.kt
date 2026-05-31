package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.PayrollRun
import com.aquinofroilan.tessera.model.PayrollRunStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PayrollRunRepository : JpaRepository<PayrollRun, String> {
    fun findByOrganizationId(organizationId: String): List<PayrollRun>

    fun findByOrganizationIdAndStatus(
        organizationId: String,
        status: PayrollRunStatus,
    ): List<PayrollRun>

    fun countByOrganizationId(organizationId: String): Long
}
