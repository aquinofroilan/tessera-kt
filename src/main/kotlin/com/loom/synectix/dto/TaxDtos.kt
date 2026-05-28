package com.loom.synectix.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class CreateTaxRateRequest(
    @field:NotBlank(message = "Tax rate name is required")
    val name: String,
    @field:NotBlank(message = "Tax rate code is required")
    val code: String,
    @field:Positive(message = "Percentage must be positive")
    val percentage: BigDecimal,
    @field:NotBlank(message = "Tax authority is required")
    val authority: String,
)

data class UpdateTaxRateRequest(
    @field:Size(min = 1, message = "Tax rate name cannot be blank")
    val name: String? = null,
    @field:Positive(message = "Percentage must be positive")
    val percentage: BigDecimal? = null,
    @field:Size(min = 1, message = "Tax authority cannot be blank")
    val authority: String? = null,
)

data class CreateTaxGroupRequest(
    @field:NotBlank(message = "Tax group name is required")
    val name: String,
    @field:NotBlank(message = "Tax group code is required")
    val code: String,
    @field:NotEmpty(message = "At least one tax rate is required")
    val taxRateIds: List<String>,
)

data class UpdateTaxGroupRequest(
    @field:Size(min = 1, message = "Tax group name cannot be blank")
    val name: String? = null,
    @field:Size(min = 1, message = "At least one tax rate is required")
    val taxRateIds: List<String>? = null,
)

data class TaxRateResponse(
    val id: String,
    val name: String,
    val code: String,
    val percentage: BigDecimal,
    val authority: String,
    val organizationId: String,
    val isActive: Boolean,
    val createdAt: String?,
    val updatedAt: String?,
)

data class TaxGroupResponse(
    val id: String,
    val name: String,
    val code: String,
    val taxRates: List<TaxRateResponse>,
    val combinedRate: BigDecimal,
    val organizationId: String,
    val isActive: Boolean,
    val createdAt: String?,
    val updatedAt: String?,
)

data class TaxSummaryResponse(
    val taxCollected: BigDecimal,
    val taxPaid: BigDecimal,
    val netTaxLiability: BigDecimal,
    val startDate: String,
    val endDate: String,
)
