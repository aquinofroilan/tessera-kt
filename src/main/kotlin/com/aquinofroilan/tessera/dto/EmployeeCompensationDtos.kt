package com.aquinofroilan.tessera.dto

import com.aquinofroilan.tessera.model.EmployeeCompensation
import com.aquinofroilan.tessera.model.PayPeriod
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate

data class CreateEmployeeCompensationRequest(
    val positionId: java.util.UUID? = null,
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
    val id: java.util.UUID,
    val employeeId: java.util.UUID,
    val positionId: java.util.UUID?,
    val payRate: BigDecimal,
    val currency: String,
    val payPeriod: PayPeriod,
    val effectiveDate: String,
    val organizationId: java.util.UUID,
    val createdBy: java.util.UUID,
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
