package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.LeaveRequest
import com.aquinofroilan.tessera.model.LeaveRequestStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface LeaveRequestRepository : JpaRepository<LeaveRequest, java.util.UUID> {
    fun findByOrganizationId(organizationId: java.util.UUID): List<LeaveRequest>

    fun findByOrganizationIdAndEmployeeId(
        organizationId: java.util.UUID,
        employeeId: java.util.UUID,
    ): List<LeaveRequest>

    fun findByOrganizationIdAndStatus(
        organizationId: java.util.UUID,
        status: LeaveRequestStatus,
    ): List<LeaveRequest>

    fun findByOrganizationIdAndEmployeeIdAndLeaveTypeIdAndStatus(
        organizationId: java.util.UUID,
        employeeId: java.util.UUID,
        leaveTypeId: java.util.UUID,
        status: LeaveRequestStatus,
    ): List<LeaveRequest>
}
