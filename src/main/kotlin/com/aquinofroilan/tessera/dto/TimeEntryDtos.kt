package com.aquinofroilan.tessera.dto

import com.aquinofroilan.tessera.model.TimeEntry
import com.aquinofroilan.tessera.model.TimeEntryStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.LocalDate

data class CreateTimeEntryRequest(
    @field:NotBlank(message = "Employee ID is required")
    val employeeId: String,
    @field:NotBlank(message = "Project ID is required")
    val projectId: String,
    val taskId: String? = null,
    @field:NotNull(message = "Entry date is required")
    val entryDate: LocalDate?,
    @field:NotNull(message = "Hours are required")
    @field:Positive(message = "Hours must be positive")
    val hours: BigDecimal?,
    val billable: Boolean = true,
    val rate: BigDecimal? = null,
    val notes: String? = null,
)

data class UpdateTimeEntryRequest(
    val taskId: String? = null,
    val entryDate: LocalDate? = null,
    @field:Positive(message = "Hours must be positive")
    val hours: BigDecimal? = null,
    val billable: Boolean? = null,
    val rate: BigDecimal? = null,
    val notes: String? = null,
)

data class TimeEntryResponse(
    val id: String,
    val employeeId: String,
    val projectId: String,
    val taskId: String?,
    val entryDate: String,
    val hours: BigDecimal,
    val billable: Boolean,
    val rate: BigDecimal?,
    val status: TimeEntryStatus,
    val notes: String?,
    val approvedBy: String?,
    val approvedAt: String?,
    val organizationId: String,
    val createdAt: String?,
    val updatedAt: String?,
) {
    companion object {
        fun from(entry: TimeEntry) =
            TimeEntryResponse(
                id = entry.id,
                employeeId = entry.employeeId,
                projectId = entry.projectId,
                taskId = entry.taskId,
                entryDate = entry.entryDate.toString(),
                hours = entry.hours,
                billable = entry.billable,
                rate = entry.rate,
                status = entry.status,
                notes = entry.notes,
                approvedBy = entry.approvedBy,
                approvedAt = entry.approvedAt?.toString(),
                organizationId = entry.organizationId,
                createdAt = entry.createdAt?.toString(),
                updatedAt = entry.updatedAt?.toString(),
            )
    }
}
