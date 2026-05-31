package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.MpsEntry
import com.aquinofroilan.tessera.model.MpsStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface MpsEntryRepository : JpaRepository<MpsEntry, String> {
    fun findByOrganizationIdOrderByRequiredByAsc(organizationId: String): List<MpsEntry>

    fun findByOrganizationIdAndStatusOrderByRequiredByAsc(
        organizationId: String,
        status: MpsStatus,
    ): List<MpsEntry>

    fun findByOrganizationIdAndRequiredByLessThanEqualAndStatusInOrderByRequiredByAsc(
        organizationId: String,
        requiredBy: LocalDate,
        statuses: Collection<MpsStatus>,
    ): List<MpsEntry>
}
