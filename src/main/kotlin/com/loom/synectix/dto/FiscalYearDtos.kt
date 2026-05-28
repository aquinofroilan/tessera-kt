package com.loom.synectix.dto

import jakarta.validation.constraints.NotBlank
import java.time.LocalDate

data class CreateFiscalYearRequest(
    @field:NotBlank(message = "Fiscal year name is required")
    val name: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
)

data class FiscalPeriodResponse(
    val id: String,
    val periodNumber: Int,
    val name: String,
    val startDate: String,
    val endDate: String,
    val status: String,
    val closedAt: String?,
    val closedBy: String?,
    val reopenedAt: String?,
    val reopenedBy: String?,
)

data class FiscalYearResponse(
    val id: String,
    val name: String,
    val startDate: String,
    val endDate: String,
    val status: String,
    val organizationId: String,
    val periods: List<FiscalPeriodResponse>,
    val closedAt: String?,
    val closedBy: String?,
    val closingEntryId: String?,
    val createdAt: String?,
    val updatedAt: String?,
)

data class FiscalYearSummaryResponse(
    val id: String,
    val name: String,
    val startDate: String,
    val endDate: String,
    val status: String,
    val organizationId: String,
    val createdAt: String?,
    val updatedAt: String?,
)
