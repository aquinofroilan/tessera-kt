package com.aquinofroilan.tessera.dto

import com.aquinofroilan.tessera.model.Lead
import com.aquinofroilan.tessera.model.LeadStatus
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.LocalDate

data class CreateLeadRequest(
    @field:NotBlank(message = "Full name is required")
    val fullName: String,
    val company: String? = null,
    @field:Email
    val email: String? = null,
    val phone: String? = null,
    val source: String? = null,
    val ownerUserId: String? = null,
    val notes: String? = null,
)

data class UpdateLeadRequest(
    val fullName: String? = null,
    val company: String? = null,
    @field:Email
    val email: String? = null,
    val phone: String? = null,
    val source: String? = null,
    val status: LeadStatus? = null,
    val ownerUserId: String? = null,
    val notes: String? = null,
)

data class ConvertLeadRequest(
    @field:NotBlank(message = "Customer ID is required for conversion")
    val customerId: String,
    val primaryContactId: String? = null,
    @field:NotBlank(message = "Stage ID is required")
    val stageId: String,
    @field:NotBlank(message = "Opportunity name is required")
    val opportunityName: String,
    @field:NotNull(message = "Amount is required")
    @field:Positive(message = "Amount must be positive")
    val amount: BigDecimal?,
    val currency: String? = null,
    val expectedCloseDate: LocalDate? = null,
    val ownerUserId: String? = null,
    val notes: String? = null,
)

data class LeadResponse(
    val id: String,
    val fullName: String,
    val company: String?,
    val email: String?,
    val phone: String?,
    val source: String?,
    val status: LeadStatus,
    val ownerUserId: String?,
    val notes: String?,
    val convertedToOpportunityId: String?,
) {
    companion object {
        fun from(l: Lead) =
            LeadResponse(
                id = l.id,
                fullName = l.fullName,
                company = l.company,
                email = l.email,
                phone = l.phone,
                source = l.source,
                status = l.status,
                ownerUserId = l.ownerUserId,
                notes = l.notes,
                convertedToOpportunityId = l.convertedToOpportunityId,
            )
    }
}
