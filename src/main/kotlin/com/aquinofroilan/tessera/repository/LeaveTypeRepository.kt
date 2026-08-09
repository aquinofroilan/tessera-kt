package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.LeaveType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface LeaveTypeRepository : JpaRepository<LeaveType, java.util.UUID> {
    fun findByOrganizationId(organizationId: java.util.UUID): List<LeaveType>

    fun findByOrganizationIdAndIsActive(
        organizationId: java.util.UUID,
        isActive: Boolean,
    ): List<LeaveType>

    fun findByOrganizationIdAndCode(
        organizationId: java.util.UUID,
        code: String,
    ): Optional<LeaveType>
}
