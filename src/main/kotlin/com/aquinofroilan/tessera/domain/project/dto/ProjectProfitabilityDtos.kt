package com.aquinofroilan.tessera.domain.project.dto

import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class ProjectProfitabilityReport(
    val projectId: UUID,
    val totalExpectedRevenue: BigDecimal,
    val totalBudgetedCost: BigDecimal,
    val actualCostToDate: BigDecimal,
    val actualBilledToDate: BigDecimal,
    val totalRecognizedRevenue: BigDecimal,
    val totalRecognizedCost: BigDecimal,
    val percentComplete: BigDecimal,
    val margin: BigDecimal,
)

data class RecognizeRevenueRequest(
    @field:NotNull(message = "Date is required")
    val date: LocalDate?,
    @field:NotNull(message = "WIP Account ID is required")
    val wipAccountId: UUID?,
    @field:NotNull(message = "Revenue Account ID is required")
    val revenueAccountId: UUID?,
    @field:NotNull(message = "Cost of Sales Account ID is required")
    val costOfSalesAccountId: UUID?,
)

data class ProjectRevenueRecognitionResponse(
    val id: UUID,
    val projectId: UUID,
    val recognizedDate: LocalDate,
    val recognizedRevenue: BigDecimal,
    val recognizedCost: BigDecimal,
    val percentComplete: BigDecimal?,
    val journalEntryId: UUID?,
    val organizationId: UUID,
    val createdBy: UUID,
    val createdAt: String?,
)
