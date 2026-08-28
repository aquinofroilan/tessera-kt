package com.aquinofroilan.tessera.domain.crm.dto

import com.aquinofroilan.tessera.domain.crm.model.CrmActivity
import com.aquinofroilan.tessera.domain.crm.model.CrmActivityType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

data class CreateActivityRequest(
    @field:NotNull(message = "Type is required")
    val type: CrmActivityType?,
    @field:NotBlank(message = "Subject is required")
    val subject: String,
    val body: String? = null,
    val relatedLeadId: java.util.UUID? = null,
    val relatedOpportunityId: java.util.UUID? = null,
    val relatedContactId: java.util.UUID? = null,
    val relatedCustomerId: java.util.UUID? = null,
    val ownerUserId: java.util.UUID? = null,
    val occurredAt: LocalDateTime? = null,
    val dueAt: LocalDateTime? = null,
)

data class UpdateActivityRequest(
    val subject: String? = null,
    val body: String? = null,
    val ownerUserId: java.util.UUID? = null,
    val occurredAt: LocalDateTime? = null,
    val dueAt: LocalDateTime? = null,
)

data class ActivityResponse(
    val id: java.util.UUID,
    val type: CrmActivityType,
    val subject: String,
    val body: String?,
    val relatedLeadId: java.util.UUID?,
    val relatedOpportunityId: java.util.UUID?,
    val relatedContactId: java.util.UUID?,
    val relatedCustomerId: java.util.UUID?,
    val ownerUserId: java.util.UUID?,
    val occurredAt: LocalDateTime,
    val dueAt: LocalDateTime?,
    val completed: Boolean,
    val completedAt: LocalDateTime?,
) {
    companion object {
        fun from(a: CrmActivity) =
            ActivityResponse(
                id = a.id,
                type = a.type,
                subject = a.subject,
                body = a.body,
                relatedLeadId = a.relatedLeadId,
                relatedOpportunityId = a.relatedOpportunityId,
                relatedContactId = a.relatedContactId,
                relatedCustomerId = a.relatedCustomerId,
                ownerUserId = a.ownerUserId,
                occurredAt = a.occurredAt,
                dueAt = a.dueAt,
                completed = a.completed,
                completedAt = a.completedAt,
            )
    }
}
