package com.aquinofroilan.tessera.dto

import jakarta.validation.constraints.NotNull
import java.time.LocalDate

/**
 * A leave request submitted by an employee for themselves. The employee is
 * resolved from the authenticated user, so no employee ID is supplied.
 */
data class SubmitSelfLeaveRequest(
    @field:NotNull(message = "Leave type ID is required")
    val leaveTypeId: java.util.UUID,
    @field:NotNull(message = "Start date is required")
    val startDate: LocalDate?,
    @field:NotNull(message = "End date is required")
    val endDate: LocalDate?,
    val reason: String? = null,
)
