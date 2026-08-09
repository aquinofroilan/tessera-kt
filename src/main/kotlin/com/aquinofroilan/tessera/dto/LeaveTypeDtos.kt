package com.aquinofroilan.tessera.dto

import com.aquinofroilan.tessera.model.LeaveType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size

data class CreateLeaveTypeRequest(
    @field:NotBlank(message = "Code is required")
    @field:Size(max = 32, message = "Code must be 32 characters or fewer")
    val code: String,
    @field:NotBlank(message = "Name is required")
    val name: String,
    val paid: Boolean = true,
    @field:PositiveOrZero(message = "Default annual days must be zero or positive")
    val defaultAnnualDays: Int = 0,
)

data class UpdateLeaveTypeRequest(
    val name: String? = null,
    val paid: Boolean? = null,
    @field:PositiveOrZero(message = "Default annual days must be zero or positive")
    val defaultAnnualDays: Int? = null,
)

data class LeaveTypeResponse(
    val id: java.util.UUID,
    val code: String,
    val name: String,
    val paid: Boolean,
    val defaultAnnualDays: Int,
    val organizationId: java.util.UUID,
    val isActive: Boolean,
    val createdAt: String?,
    val updatedAt: String?,
) {
    companion object {
        fun from(leaveType: LeaveType) =
            LeaveTypeResponse(
                id = leaveType.id,
                code = leaveType.code,
                name = leaveType.name,
                paid = leaveType.paid,
                defaultAnnualDays = leaveType.defaultAnnualDays,
                organizationId = leaveType.organizationId,
                isActive = leaveType.isActive,
                createdAt = leaveType.createdAt?.toString(),
                updatedAt = leaveType.updatedAt?.toString(),
            )
    }
}
