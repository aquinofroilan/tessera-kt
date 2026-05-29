package com.loom.synectix.repository

import com.loom.synectix.model.LeaveRequest
import com.loom.synectix.model.LeaveRequestStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface LeaveRequestRepository : JpaRepository<LeaveRequest, String> {
    fun findByOrganizationId(organizationId: String): List<LeaveRequest>

    fun findByOrganizationIdAndEmployeeId(
        organizationId: String,
        employeeId: String,
    ): List<LeaveRequest>

    fun findByOrganizationIdAndStatus(
        organizationId: String,
        status: LeaveRequestStatus,
    ): List<LeaveRequest>

    fun findByOrganizationIdAndEmployeeIdAndLeaveTypeIdAndStatus(
        organizationId: String,
        employeeId: String,
        leaveTypeId: String,
        status: LeaveRequestStatus,
    ): List<LeaveRequest>
}
