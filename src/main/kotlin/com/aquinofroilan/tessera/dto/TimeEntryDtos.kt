package com.aquinofroilan.tessera.dto

import com.aquinofroilan.tessera.model.TimeEntry
import com.aquinofroilan.tessera.model.TimeEntryStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.LocalDate

data class CreateTimeEntryRequest(
    @field:NotNull(message = "Employee ID is required")
    val employeeId: java.util.UUID,
    @field:NotNull(message = "Project ID is required")
    val projectId: java.util.UUID,
    val taskId: java.util.UUID? = null,
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
    val taskId: java.util.UUID? = null,
    val entryDate: LocalDate? = null,
    @field:Positive(message = "Hours must be positive")
    val hours: BigDecimal? = null,
    val billable: Boolean? = null,
    val rate: BigDecimal? = null,
    val notes: String? = null,
)

data class TimeEntryResponse(
    val id: java.util.UUID,
    val employeeId: java.util.UUID,
    val projectId: java.util.UUID,
    val taskId: java.util.UUID?,
    val entryDate: String,
    val hours: BigDecimal,
    val billable: Boolean,
    val rate: BigDecimal?,
    val status: TimeEntryStatus,
    val notes: String?,
    val approvedBy: java.util.UUID?,
    val approvedAt: String?,
    val organizationId: java.util.UUID,
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
