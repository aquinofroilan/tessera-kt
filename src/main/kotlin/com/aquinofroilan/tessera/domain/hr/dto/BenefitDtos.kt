package com.aquinofroilan.tessera.domain.hr.dto

import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

data class CreateBenefitPlanRequest(
    @field:NotEmpty(message = "Name is required")
    val name: String?,
    val description: String? = null,
    @field:NotNull(message = "Employee contribution is required")
    val employeeContribution: BigDecimal?,
    @field:NotNull(message = "Employer contribution is required")
    val employerContribution: BigDecimal?,
)

data class EnrollBenefitRequest(
    @field:NotNull(message = "Employee ID is required")
    val employeeId: java.util.UUID?,
    @field:NotNull(message = "Plan ID is required")
    val planId: java.util.UUID?,
)
