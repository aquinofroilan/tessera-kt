package com.aquinofroilan.tessera.dto

import com.aquinofroilan.tessera.model.LeaveRequest
import com.aquinofroilan.tessera.model.LeaveRequestStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

data class CreateLeaveRequestRequest(
    @field:NotBlank(message = "Employee ID is required")
    val employeeId: String,
    @field:NotBlank(message = "Leave type ID is required")
    val leaveTypeId: String,
    @field:NotNull(message = "Start date is required")
    val startDate: LocalDate?,
    @field:NotNull(message = "End date is required")
    val endDate: LocalDate?,
    val reason: String? = null,
)

data class RejectLeaveRequestRequest(
    val reason: String? = null,
)

data class LeaveRequestResponse(
    val id: String,
    val employeeId: String,
    val leaveTypeId: String,
    val startDate: String,
    val endDate: String,
    val days: Int,
    val reason: String?,
    val status: LeaveRequestStatus,
    val decisionReason: String?,
    val decidedBy: String?,
    val decidedAt: String?,
    val organizationId: String,
    val requestedBy: String,
    val createdAt: String?,
    val updatedAt: String?,
) {
    companion object {
        fun from(request: LeaveRequest) =
            LeaveRequestResponse(
                id = request.id,
                employeeId = request.employeeId,
                leaveTypeId = request.leaveTypeId,
                startDate = request.startDate.toString(),
                endDate = request.endDate.toString(),
                days = request.days,
                reason = request.reason,
                status = request.status,
                decisionReason = request.decisionReason,
                decidedBy = request.decidedBy,
                decidedAt = request.decidedAt?.toString(),
                organizationId = request.organizationId,
                requestedBy = request.requestedBy,
                createdAt = request.createdAt?.toString(),
                updatedAt = request.updatedAt?.toString(),
            )
    }
}

data class LeaveBalanceResponse(
    val employeeId: String,
    val leaveTypeId: String,
    val year: Int,
    val entitlementDays: Int,
    val usedDays: Int,
    val remainingDays: Int,
)
