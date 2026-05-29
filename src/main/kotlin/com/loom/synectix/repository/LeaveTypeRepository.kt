package com.loom.synectix.repository

import com.loom.synectix.model.LeaveType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface LeaveTypeRepository : JpaRepository<LeaveType, String> {
    fun findByOrganizationId(organizationId: String): List<LeaveType>

    fun findByOrganizationIdAndIsActive(
        organizationId: String,
        isActive: Boolean,
    ): List<LeaveType>

    fun findByOrganizationIdAndCode(
        organizationId: String,
        code: String,
    ): Optional<LeaveType>
}
