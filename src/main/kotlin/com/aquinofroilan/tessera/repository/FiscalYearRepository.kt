package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.FiscalYear
import com.aquinofroilan.tessera.model.FiscalYearStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface FiscalYearRepository : JpaRepository<FiscalYear, java.util.UUID> {
    fun findByOrganizationId(organizationId: java.util.UUID): List<FiscalYear>

    fun findByOrganizationIdAndStatus(
        organizationId: java.util.UUID,
        status: FiscalYearStatus,
    ): List<FiscalYear>
}
