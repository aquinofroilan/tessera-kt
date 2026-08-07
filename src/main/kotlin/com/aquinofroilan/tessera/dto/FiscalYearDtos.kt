package com.aquinofroilan.tessera.dto

import jakarta.validation.constraints.NotBlank
import java.time.LocalDate

data class CreateFiscalYearRequest(
    @field:NotBlank(message = "Fiscal year name is required")
    val name: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
)

data class FiscalPeriodResponse(
    val id: java.util.UUID,
    val periodNumber: Int,
    val name: String,
    val startDate: String,
    val endDate: String,
    val status: String,
    val closedAt: String?,
    val closedBy: java.util.UUID?,
    val reopenedAt: String?,
    val reopenedBy: java.util.UUID?,
)

data class FiscalYearResponse(
    val id: java.util.UUID,
    val name: String,
    val startDate: String,
    val endDate: String,
    val status: String,
    val organizationId: java.util.UUID,
    val periods: List<FiscalPeriodResponse>,
    val closedAt: String?,
    val closedBy: java.util.UUID?,
    val closingEntryId: java.util.UUID?,
    val createdAt: String?,
    val updatedAt: String?,
)

data class FiscalYearSummaryResponse(
    val id: java.util.UUID,
    val name: String,
    val startDate: String,
    val endDate: String,
    val status: String,
    val organizationId: java.util.UUID,
    val createdAt: String?,
    val updatedAt: String?,
)
