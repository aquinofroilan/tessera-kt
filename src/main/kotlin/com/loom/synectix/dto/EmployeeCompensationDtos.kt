package com.loom.synectix.dto

import com.loom.synectix.model.EmployeeCompensation
import com.loom.synectix.model.PayPeriod
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate

data class CreateEmployeeCompensationRequest(
    val positionId: String? = null,
    @field:NotNull(message = "Pay rate is required")
    @field:Positive(message = "Pay rate must be positive")
    val payRate: BigDecimal?,
    @field:Size(min = 3, max = 3, message = "Currency must be a 3-letter code")
    val currency: String,
    @field:NotNull(message = "Pay period is required")
    val payPeriod: PayPeriod?,
    @field:NotNull(message = "Effective date is required")
    val effectiveDate: LocalDate?,
)

data class EmployeeCompensationResponse(
    val id: String,
    val employeeId: String,
    val positionId: String?,
    val payRate: BigDecimal,
    val currency: String,
    val payPeriod: PayPeriod,
    val effectiveDate: String,
    val organizationId: String,
    val createdBy: String,
    val createdAt: String?,
) {
    companion object {
        fun from(comp: EmployeeCompensation) =
            EmployeeCompensationResponse(
                id = comp.id,
                employeeId = comp.employeeId,
                positionId = comp.positionId,
                payRate = comp.payRate,
                currency = comp.currency,
                payPeriod = comp.payPeriod,
                effectiveDate = comp.effectiveDate.toString(),
                organizationId = comp.organizationId,
                createdBy = comp.createdBy,
                createdAt = comp.createdAt?.toString(),
            )
    }
}
