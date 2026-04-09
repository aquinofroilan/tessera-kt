package com.froilan.synectix.repository

import com.froilan.synectix.model.FiscalYear
import com.froilan.synectix.model.FiscalYearStatus
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface FiscalYearRepository : MongoRepository<FiscalYear, String> {
    fun findByOrganizationId(organizationId: String): List<FiscalYear>

    fun findByOrganizationIdAndStatus(
        organizationId: String,
        status: FiscalYearStatus,
    ): List<FiscalYear>
}
