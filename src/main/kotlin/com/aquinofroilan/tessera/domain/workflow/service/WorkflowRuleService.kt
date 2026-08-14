package com.aquinofroilan.tessera.domain.workflow.service

import com.aquinofroilan.tessera.domain.workflow.dto.CreateWorkflowRuleRequest
import com.aquinofroilan.tessera.domain.workflow.dto.UpdateWorkflowRuleRequest
import com.aquinofroilan.tessera.domain.workflow.model.WorkflowRule
import com.aquinofroilan.tessera.domain.workflow.repository.WorkflowRuleRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class WorkflowRuleService(
    private val workflowRuleRepository: WorkflowRuleRepository,
) {
    @Transactional
    fun createRule(
        request: CreateWorkflowRuleRequest,
        organizationId: java.util.UUID,
    ): WorkflowRule {
        val actionType = request.actionType ?: throw BusinessRuleException("Action type is required")
        return workflowRuleRepository.save(
            WorkflowRule(
                organizationId = organizationId,
                name = request.name.trim(),
                description = request.description?.trim()?.takeIf { it.isNotEmpty() },
                eventKind = request.eventKind.trim(),
                actionType = actionType,
                actionTarget = request.actionTarget.trim(),
                enabled = request.enabled,
            ),
        )
    }

    fun listRules(
        organizationId: java.util.UUID,
        eventKind: String? = null,
    ): List<WorkflowRule> =
        if (eventKind != null) {
            workflowRuleRepository.findByOrganizationIdAndEventKind(organizationId, eventKind)
        } else {
            workflowRuleRepository.findByOrganizationId(organizationId)
        }

    fun getRule(
        id: java.util.UUID,
        organizationId: java.util.UUID,
    ): WorkflowRule {
        val rule =
            workflowRuleRepository.findById(id).orElseThrow {
                ResourceNotFoundException("Workflow rule $id not found")
            }
        if (rule.organizationId != organizationId) {
            throw ResourceNotFoundException("Workflow rule $id not found")
        }
        return rule
    }

    fun findEnabledFor(
        organizationId: java.util.UUID,
        eventKind: String,
    ): List<WorkflowRule> = workflowRuleRepository.findByOrganizationIdAndEventKindAndEnabledTrue(organizationId, eventKind)

    @Transactional
    fun updateRule(
        id: java.util.UUID,
        organizationId: java.util.UUID,
        request: UpdateWorkflowRuleRequest,
    ): WorkflowRule {
        val existing = getRule(id, organizationId)
        val updated =
            existing.copy(
                name = request.name?.trim()?.takeIf { it.isNotEmpty() } ?: existing.name,
                description =
                    when (val d = request.description) {
                        null -> existing.description
                        else -> d.trim().takeIf { it.isNotEmpty() }
                    },
                eventKind = request.eventKind?.trim()?.takeIf { it.isNotEmpty() } ?: existing.eventKind,
                actionType = request.actionType ?: existing.actionType,
                actionTarget = request.actionTarget?.trim()?.takeIf { it.isNotEmpty() } ?: existing.actionTarget,
                enabled = request.enabled ?: existing.enabled,
            )
        return workflowRuleRepository.save(updated)
    }

    @Transactional
    fun deleteRule(
        id: java.util.UUID,
        organizationId: java.util.UUID,
    ) {
        val existing = getRule(id, organizationId)
        workflowRuleRepository.delete(existing)
    }
}
