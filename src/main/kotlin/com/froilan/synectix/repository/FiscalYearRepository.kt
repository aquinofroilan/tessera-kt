package com.froilan.synectix.repository

import com.froilan.synectix.model.FiscalYear
import com.froilan.synectix.model.FiscalYearStatus
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
