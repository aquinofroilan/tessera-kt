package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.FiscalYear
import com.aquinofroilan.tessera.model.FiscalYearStatus
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
