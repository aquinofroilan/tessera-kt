package com.aquinofroilan.tessera.dto

import com.aquinofroilan.tessera.model.CrmActivity
import com.aquinofroilan.tessera.model.CrmActivityType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

data class CreateActivityRequest(
    @field:NotNull(message = "Type is required")
    val type: CrmActivityType?,
    @field:NotBlank(message = "Subject is required")
    val subject: String,
    val body: String? = null,
    val relatedLeadId: String? = null,
    val relatedOpportunityId: String? = null,
    val relatedContactId: String? = null,
    val relatedCustomerId: String? = null,
    val ownerUserId: String? = null,
    val occurredAt: LocalDateTime? = null,
    val dueAt: LocalDateTime? = null,
)

data class UpdateActivityRequest(
    val subject: String? = null,
    val body: String? = null,
    val ownerUserId: String? = null,
    val occurredAt: LocalDateTime? = null,
    val dueAt: LocalDateTime? = null,
)

data class ActivityResponse(
    val id: String,
    val type: CrmActivityType,
    val subject: String,
    val body: String?,
    val relatedLeadId: String?,
    val relatedOpportunityId: String?,
    val relatedContactId: String?,
    val relatedCustomerId: String?,
    val ownerUserId: String?,
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
