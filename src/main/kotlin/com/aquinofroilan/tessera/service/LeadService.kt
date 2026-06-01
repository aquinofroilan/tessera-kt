package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.ConvertLeadRequest
import com.aquinofroilan.tessera.dto.CreateLeadRequest
import com.aquinofroilan.tessera.dto.CreateOpportunityRequest
import com.aquinofroilan.tessera.dto.UpdateLeadRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.Lead
import com.aquinofroilan.tessera.model.LeadStatus
import com.aquinofroilan.tessera.model.Opportunity
import com.aquinofroilan.tessera.repository.LeadRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class LeadService(
    private val leadRepository: LeadRepository,
    private val opportunityService: OpportunityService,
) {
    @Transactional
    fun createLead(
        request: CreateLeadRequest,
        organizationId: String,
        userId: String,
    ): Lead {
        if (request.fullName.isBlank()) {
            throw BusinessRuleException("Full name is required")
        }
        return leadRepository.save(
            Lead(
                organizationId = organizationId,
                fullName = request.fullName.trim(),
                company = request.company?.trim(),
                email = request.email?.trim(),
                phone = request.phone?.trim(),
                source = request.source?.trim(),
                ownerUserId = request.ownerUserId,
                notes = request.notes,
                createdBy = userId,
            ),
        )
    }

    fun getLead(
        id: String,
        organizationId: String,
    ): Lead {
        val l =
            leadRepository.findById(id).orElseThrow {
                ResourceNotFoundException("Lead not found: $id")
            }
        if (l.organizationId != organizationId) {
            throw ResourceNotFoundException("Lead not found: $id")
        }
        return l
    }

    fun listLeads(
        organizationId: String,
        status: LeadStatus?,
        ownerUserId: String?,
    ): List<Lead> =
        when {
            status != null -> leadRepository.findByOrganizationIdAndStatus(organizationId, status)
            ownerUserId != null -> leadRepository.findByOrganizationIdAndOwnerUserId(organizationId, ownerUserId)
            else -> leadRepository.findByOrganizationId(organizationId)
        }

    @Transactional
    fun updateLead(
        id: String,
        request: UpdateLeadRequest,
        organizationId: String,
    ): Lead {
        val l = getLead(id, organizationId)
        if (l.status == LeadStatus.CONVERTED) {
            throw BusinessRuleException("Cannot edit a CONVERTED lead")
        }
        if (request.status == LeadStatus.CONVERTED) {
            throw BusinessRuleException("Use the convert endpoint to convert a lead")
        }
        return leadRepository.save(
            l.copy(
                fullName = request.fullName?.trim() ?: l.fullName,
                company = request.company?.trim() ?: l.company,
                email = request.email?.trim() ?: l.email,
                phone = request.phone?.trim() ?: l.phone,
                source = request.source?.trim() ?: l.source,
                status = request.status ?: l.status,
                ownerUserId = request.ownerUserId ?: l.ownerUserId,
                notes = request.notes ?: l.notes,
            ),
        )
    }

    @Transactional
    fun convertLead(
        id: String,
        request: ConvertLeadRequest,
        organizationId: String,
        userId: String,
    ): Pair<Lead, Opportunity> {
        val lead = getLead(id, organizationId)
        if (lead.status == LeadStatus.CONVERTED) {
            throw BusinessRuleException("Lead is already converted to opportunity ${lead.convertedToOpportunityId}")
        }
        if (lead.status == LeadStatus.DISQUALIFIED) {
            throw BusinessRuleException("Cannot convert a DISQUALIFIED lead; re-qualify first")
        }
        val opportunity =
            opportunityService.createOpportunity(
                CreateOpportunityRequest(
                    name = request.opportunityName,
                    customerId = request.customerId,
                    primaryContactId = request.primaryContactId,
                    stageId = request.stageId,
                    amount = request.amount,
                    currency = request.currency,
                    expectedCloseDate = request.expectedCloseDate,
                    ownerUserId = request.ownerUserId ?: lead.ownerUserId,
                    notes = request.notes,
                ),
                organizationId,
                userId,
                sourceLeadId = lead.id,
            )
        val saved =
            leadRepository.save(
                lead.copy(
                    status = LeadStatus.CONVERTED,
                    convertedToOpportunityId = opportunity.id,
                    convertedAt = LocalDateTime.now(),
                ),
            )
        return saved to opportunity
    }

    @Transactional
    fun disqualifyLead(
        id: String,
        organizationId: String,
    ): Lead {
        val l = getLead(id, organizationId)
        if (l.status == LeadStatus.CONVERTED) {
            throw BusinessRuleException("Cannot disqualify a CONVERTED lead")
        }
        if (l.status == LeadStatus.DISQUALIFIED) return l
        return leadRepository.save(l.copy(status = LeadStatus.DISQUALIFIED))
    }
}
