package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateActivityRequest
import com.aquinofroilan.tessera.dto.UpdateActivityRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.CrmActivity
import com.aquinofroilan.tessera.model.CrmActivityType
import com.aquinofroilan.tessera.repository.CrmActivityRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class CrmActivityService(
    private val activityRepository: CrmActivityRepository,
    private val leadService: LeadService,
    private val opportunityService: OpportunityService,
    private val contactService: ContactService,
    private val customerService: CustomerService,
) {
    @Transactional
    fun createActivity(
        request: CreateActivityRequest,
        organizationId: java.util.UUID,
        userId: java.util.UUID,
    ): CrmActivity {
        val type = request.type ?: throw BusinessRuleException("Type is required")
        if (request.relatedLeadId == null &&
            request.relatedOpportunityId == null &&
            request.relatedContactId == null &&
            request.relatedCustomerId == null
        ) {
            throw BusinessRuleException("Activity must be linked to at least one of lead, opportunity, contact, or customer")
        }
        if (type == CrmActivityType.TASK && request.dueAt == null) {
            throw BusinessRuleException("TASK activities require a dueAt timestamp")
        }
        request.relatedLeadId?.let { leadService.getLead(it, organizationId) }
        request.relatedOpportunityId?.let { opportunityService.getOpportunity(it, organizationId) }
        request.relatedContactId?.let { contactService.getContact(it, organizationId) }
        request.relatedCustomerId?.let { customerService.getCustomer(it, organizationId) }

        return activityRepository.save(
            CrmActivity(
                organizationId = organizationId,
                type = type,
                subject = request.subject.trim(),
                body = request.body,
                relatedLeadId = request.relatedLeadId,
                relatedOpportunityId = request.relatedOpportunityId,
                relatedContactId = request.relatedContactId,
                relatedCustomerId = request.relatedCustomerId,
                ownerUserId = request.ownerUserId ?: userId,
                occurredAt = request.occurredAt ?: LocalDateTime.now(),
                dueAt = request.dueAt,
                createdBy = userId,
            ),
        )
    }

    fun getActivity(
        id: java.util.UUID,
        organizationId: java.util.UUID,
    ): CrmActivity {
        val a =
            activityRepository.findById(id).orElseThrow {
                ResourceNotFoundException("Activity not found: $id")
            }
        if (a.organizationId != organizationId) {
            throw ResourceNotFoundException("Activity not found: $id")
        }
        return a
    }

    fun listActivities(
        organizationId: java.util.UUID,
        type: CrmActivityType?,
        leadId: java.util.UUID?,
        opportunityId: java.util.UUID?,
        contactId: java.util.UUID?,
        customerId: java.util.UUID?,
    ): List<CrmActivity> =
        when {
            leadId != null ->
                activityRepository.findByOrganizationIdAndRelatedLeadIdOrderByOccurredAtDesc(organizationId, leadId)
            opportunityId != null ->
                activityRepository.findByOrganizationIdAndRelatedOpportunityIdOrderByOccurredAtDesc(organizationId, opportunityId)
            contactId != null ->
                activityRepository.findByOrganizationIdAndRelatedContactIdOrderByOccurredAtDesc(organizationId, contactId)
            customerId != null ->
                activityRepository.findByOrganizationIdAndRelatedCustomerIdOrderByOccurredAtDesc(organizationId, customerId)
            type != null ->
                activityRepository.findByOrganizationIdAndTypeOrderByOccurredAtDesc(organizationId, type)
            else ->
                activityRepository.findByOrganizationIdOrderByOccurredAtDesc(organizationId)
        }

    fun listMyOpenTasks(
        organizationId: java.util.UUID,
        userId: java.util.UUID,
    ): List<CrmActivity> = activityRepository.findByOrganizationIdAndOwnerUserIdAndCompletedFalseOrderByDueAtAsc(organizationId, userId)

    @Transactional
    fun updateActivity(
        id: java.util.UUID,
        request: UpdateActivityRequest,
        organizationId: java.util.UUID,
    ): CrmActivity {
        val a = getActivity(id, organizationId)
        if (a.completed) {
            throw BusinessRuleException("Cannot edit a completed activity")
        }
        return activityRepository.save(
            a.copy(
                subject = request.subject?.trim() ?: a.subject,
                body = request.body ?: a.body,
                ownerUserId = request.ownerUserId ?: a.ownerUserId,
                occurredAt = request.occurredAt ?: a.occurredAt,
                dueAt = request.dueAt ?: a.dueAt,
            ),
        )
    }

    @Transactional
    fun completeActivity(
        id: java.util.UUID,
        organizationId: java.util.UUID,
        userId: java.util.UUID,
    ): CrmActivity {
        val a = getActivity(id, organizationId)
        if (a.completed) return a
        if (a.type != CrmActivityType.TASK) {
            throw BusinessRuleException("Only TASK activities can be completed")
        }
        return activityRepository.save(
            a.copy(
                completed = true,
                completedAt = LocalDateTime.now(),
                completedBy = userId,
            ),
        )
    }

    @Transactional
    fun deleteActivity(
        id: java.util.UUID,
        organizationId: java.util.UUID,
    ) {
        val a = getActivity(id, organizationId)
        activityRepository.delete(a)
    }
}
