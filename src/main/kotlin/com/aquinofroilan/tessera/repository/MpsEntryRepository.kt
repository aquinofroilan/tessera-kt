package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.MpsEntry
import com.aquinofroilan.tessera.model.MpsStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface MpsEntryRepository : JpaRepository<MpsEntry, java.util.UUID> {
    fun findByOrganizationIdOrderByRequiredByAsc(organizationId: java.util.UUID): List<MpsEntry>

    fun findByOrganizationIdAndStatusOrderByRequiredByAsc(
        organizationId: java.util.UUID,
        status: MpsStatus,
    ): List<MpsEntry>

    fun findByOrganizationIdAndRequiredByLessThanEqualAndStatusInOrderByRequiredByAsc(
        organizationId: java.util.UUID,
        requiredBy: LocalDate,
        statuses: Collection<MpsStatus>,
    ): List<MpsEntry>
}
