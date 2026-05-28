package com.loom.synectix.repository

import com.loom.synectix.model.FiscalYear
import com.loom.synectix.model.FiscalYearStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface FiscalYearRepository : JpaRepository<FiscalYear, String> {
    fun findByOrganizationId(organizationId: String): List<FiscalYear>

    fun findByOrganizationIdAndStatus(
        organizationId: String,
        status: FiscalYearStatus,
    ): List<FiscalYear>
}
