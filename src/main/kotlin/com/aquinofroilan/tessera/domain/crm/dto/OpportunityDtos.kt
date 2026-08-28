package com.aquinofroilan.tessera.domain.crm.dto

import com.aquinofroilan.tessera.domain.crm.model.Opportunity
import com.aquinofroilan.tessera.domain.crm.model.OpportunityStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class CreateOpportunityRequest(
    @field:NotBlank(message = "Opportunity name is required")
    val name: String,
    @field:NotBlank(message = "Customer ID is required")
    val customerId: UUID,
    val primaryContactId: UUID? = null,
    @field:NotBlank(message = "Stage ID is required")
    val stageId: UUID,
    @field:NotNull(message = "Amount is required")
    @field:PositiveOrZero(message = "Amount cannot be negative")
    val amount: BigDecimal?,
    val currency: String? = null,
    val expectedCloseDate: LocalDate? = null,
    val ownerUserId: UUID? = null,
    val notes: String? = null,
)

data class UpdateOpportunityRequest(
    val name: String? = null,
    val primaryContactId: UUID? = null,
    val stageId: UUID? = null,
    @field:PositiveOrZero
    val amount: BigDecimal? = null,
    val currency: String? = null,
    val expectedCloseDate: LocalDate? = null,
    val ownerUserId: UUID? = null,
    val notes: String? = null,
)

data class CloseOpportunityRequest(
    @field:NotBlank(message = "stageId for the terminal stage is required (e.g. WON or LOST)")
    val stageId: UUID,
    val notes: String? = null,
)

data class OpportunityResponse(
    val id: UUID,
    val name: String,
    val customerId: UUID,
    val primaryContactId: UUID?,
    val stageId: UUID,
    val amount: BigDecimal,
    val currency: String,
    val expectedCloseDate: LocalDate?,
    val status: OpportunityStatus,
    val ownerUserId: UUID?,
    val sourceLeadId: UUID?,
    val notes: String?,
) {
    companion object {
        fun from(o: Opportunity) =
            OpportunityResponse(
                id = o.id,
                name = o.name,
                customerId = o.customerId,
                primaryContactId = o.primaryContactId,
                stageId = o.stageId,
                amount = o.amount,
                currency = o.currency,
                expectedCloseDate = o.expectedCloseDate,
                status = o.status,
                ownerUserId = o.ownerUserId,
                sourceLeadId = o.sourceLeadId,
                notes = o.notes,
            )
    }
}
