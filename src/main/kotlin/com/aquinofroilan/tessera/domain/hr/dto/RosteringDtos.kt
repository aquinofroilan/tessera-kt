package com.aquinofroilan.tessera.domain.hr.dto

import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

data class CreateShiftRequest(
    @field:NotEmpty(message = "Name is required")
    val name: String?,
    @field:NotNull(message = "Start time is required")
    val startTime: LocalTime?,
    @field:NotNull(message = "End time is required")
    val endTime: LocalTime?,
)

data class CreateRosterRequest(
    @field:NotEmpty(message = "Name is required")
    val name: String?,
    @field:NotNull(message = "Start date is required")
    val startDate: LocalDate?,
    @field:NotNull(message = "End date is required")
    val endDate: LocalDate?,
    val entries: List<RosterEntryDto> = emptyList(),
)

data class RosterEntryDto(
    @field:NotNull(message = "Employee ID is required")
    val employeeId: UUID?,
    @field:NotNull(message = "Shift ID is required")
    val shiftId: UUID?,
    @field:NotNull(message = "Shift date is required")
    val shiftDate: LocalDate?,
)
