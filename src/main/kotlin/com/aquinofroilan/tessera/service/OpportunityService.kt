package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CloseOpportunityRequest
import com.aquinofroilan.tessera.dto.CreateOpportunityRequest
import com.aquinofroilan.tessera.dto.UpdateOpportunityRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.Opportunity
import com.aquinofroilan.tessera.model.OpportunityStatus
import com.aquinofroilan.tessera.repository.OpportunityRepository
import com.aquinofroilan.tessera.repository.OrganizationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class OpportunityService(
    private val opportunityRepository: OpportunityRepository,
    private val customerService: CustomerService,
    private val contactService: ContactService,
    private val pipelineStageService: PipelineStageService,
    private val organizationRepository: OrganizationRepository,
) {
    @Transactional
    fun createOpportunity(
        request: CreateOpportunityRequest,
        organizationId: String,
        createdBy: String,
        sourceLeadId: String? = null,
    ): Opportunity {
        val customer = customerService.getCustomer(request.customerId, organizationId)
        if (!customer.isActive) {
            throw BusinessRuleException("Customer is inactive")
        }
        val stage = pipelineStageService.getStage(request.stageId, organizationId)
        if (!stage.isActive) {
            throw BusinessRuleException("Pipeline stage '${stage.code}' is inactive")
        }
        if (stage.isWon || stage.isLost) {
            throw BusinessRuleException("New opportunities must start in a non-terminal stage")
        }
        if (request.primaryContactId != null) {
            val c = contactService.getContact(request.primaryContactId, organizationId)
            if (c.customerId != null && c.customerId != customer.id) {
                throw BusinessRuleException("Contact is linked to a different customer")
            }
        }
        val amount = request.amount ?: throw BusinessRuleException("Amount is required")
        val currency = request.currency?.uppercase() ?: orgCurrency(organizationId)
        return opportunityRepository.save(
            Opportunity(
                organizationId = organizationId,
                name = request.name.trim(),
                customerId = customer.id,
                primaryContactId = request.primaryContactId,
                stageId = stage.id,
                amount = amount,
                currency = currency,
                expectedCloseDate = request.expectedCloseDate,
                ownerUserId = request.ownerUserId,
                sourceLeadId = sourceLeadId,
                notes = request.notes,
                createdBy = createdBy,
            ),
        )
    }

    fun getOpportunity(
        id: String,
        organizationId: String,
    ): Opportunity {
        val o =
            opportunityRepository.findById(id).orElseThrow {
                ResourceNotFoundException("Opportunity not found: $id")
            }
        if (o.organizationId != organizationId) {
            throw ResourceNotFoundException("Opportunity not found: $id")
        }
        return o
    }

    fun listOpportunities(
        organizationId: String,
        status: OpportunityStatus?,
        customerId: String?,
        stageId: String?,
        ownerUserId: String?,
    ): List<Opportunity> =
        when {
            status != null -> opportunityRepository.findByOrganizationIdAndStatus(organizationId, status)
            customerId != null -> opportunityRepository.findByOrganizationIdAndCustomerId(organizationId, customerId)
            stageId != null -> opportunityRepository.findByOrganizationIdAndStageId(organizationId, stageId)
            ownerUserId != null -> opportunityRepository.findByOrganizationIdAndOwnerUserId(organizationId, ownerUserId)
            else -> opportunityRepository.findByOrganizationId(organizationId)
        }

    @Transactional
    fun updateOpportunity(
        id: String,
        request: UpdateOpportunityRequest,
        organizationId: String,
    ): Opportunity {
        val o = getOpportunity(id, organizationId)
        if (o.status != OpportunityStatus.OPEN) {
            throw BusinessRuleException("Cannot edit a ${o.status} opportunity")
        }
        if (request.stageId != null && request.stageId != o.stageId) {
            val stage = pipelineStageService.getStage(request.stageId, organizationId)
            if (!stage.isActive) throw BusinessRuleException("Pipeline stage '${stage.code}' is inactive")
            if (stage.isWon || stage.isLost) {
                throw BusinessRuleException("Use the close endpoint to move into a terminal stage")
            }
        }
        if (request.primaryContactId != null && request.primaryContactId != o.primaryContactId) {
            val c = contactService.getContact(request.primaryContactId, organizationId)
            if (c.customerId != null && c.customerId != o.customerId) {
                throw BusinessRuleException("Contact is linked to a different customer")
            }
        }
        return opportunityRepository.save(
            o.copy(
                name = request.name?.trim() ?: o.name,
                primaryContactId = request.primaryContactId ?: o.primaryContactId,
                stageId = request.stageId ?: o.stageId,
                amount = request.amount ?: o.amount,
                currency = request.currency?.uppercase() ?: o.currency,
                expectedCloseDate = request.expectedCloseDate ?: o.expectedCloseDate,
                ownerUserId = request.ownerUserId ?: o.ownerUserId,
                notes = request.notes ?: o.notes,
            ),
        )
    }

    @Transactional
    fun closeOpportunity(
        id: String,
        request: CloseOpportunityRequest,
        organizationId: String,
        userId: String,
    ): Opportunity {
        val o = getOpportunity(id, organizationId)
        if (o.status != OpportunityStatus.OPEN) {
            throw BusinessRuleException("Opportunity is already ${o.status}")
        }
        val stage = pipelineStageService.getStage(request.stageId, organizationId)
        if (!stage.isWon && !stage.isLost) {
            throw BusinessRuleException("Closing stage must be flagged is_won or is_lost")
        }
        val nextStatus = if (stage.isWon) OpportunityStatus.WON else OpportunityStatus.LOST
        return opportunityRepository.save(
            o.copy(
                stageId = stage.id,
                status = nextStatus,
                notes = request.notes ?: o.notes,
                closedAt = LocalDateTime.now(),
                closedBy = userId,
            ),
        )
    }

    @Transactional
    fun abandonOpportunity(
        id: String,
        organizationId: String,
        userId: String,
    ): Opportunity {
        val o = getOpportunity(id, organizationId)
        if (o.status != OpportunityStatus.OPEN) {
            throw BusinessRuleException("Opportunity is already ${o.status}")
        }
        return opportunityRepository.save(
            o.copy(
                status = OpportunityStatus.ABANDONED,
                closedAt = LocalDateTime.now(),
                closedBy = userId,
            ),
        )
    }

    private fun orgCurrency(organizationId: String): String =
        organizationRepository.findById(organizationId).map { it.baseCurrency }.orElse("USD")

    fun amountZero(): BigDecimal = BigDecimal.ZERO
}
