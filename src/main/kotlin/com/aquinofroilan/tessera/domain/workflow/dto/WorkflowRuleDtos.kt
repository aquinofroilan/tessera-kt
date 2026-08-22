package com.aquinofroilan.tessera.domain.workflow.dto

import com.aquinofroilan.tessera.domain.workflow.model.WorkflowRule
import com.aquinofroilan.tessera.domain.workflow.model.WorkflowRuleActionType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class CreateWorkflowRuleRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(max = 200)
    val name: String,
    @field:Size(max = 1000)
    val description: String? = null,
    @field:NotBlank(message = "Event kind is required")
    @field:Size(max = 100)
    val eventKind: String,
    @field:NotNull(message = "Action type is required")
    val actionType: WorkflowRuleActionType?,
    @field:NotBlank(message = "Action target is required")
    @field:Size(max = 200)
    val actionTarget: String,
    val enabled: Boolean = true,
)

data class UpdateWorkflowRuleRequest(
    @field:Size(max = 200)
    val name: String? = null,
    @field:Size(max = 1000)
    val description: String? = null,
    @field:Size(max = 100)
    val eventKind: String? = null,
    val actionType: WorkflowRuleActionType? = null,
    @field:Size(max = 200)
    val actionTarget: String? = null,
    val enabled: Boolean? = null,
)

data class WorkflowRuleResponse(
    val id: java.util.UUID,
    val name: String,
    val description: String?,
    val eventKind: String,
    val actionType: WorkflowRuleActionType,
    val actionTarget: String,
    val enabled: Boolean,
    val createdAt: String?,
    val updatedAt: String?,
) {
    companion object {
        fun from(rule: WorkflowRule) =
            WorkflowRuleResponse(
                id = rule.id,
                name = rule.name,
                description = rule.description,
                eventKind = rule.eventKind,
                actionType = rule.actionType,
                actionTarget = rule.actionTarget,
                enabled = rule.enabled,
                createdAt = rule.createdAt?.toString(),
                updatedAt = (rule.updatedAt ?: rule.createdAt)?.toString(),
            )
    }
}
