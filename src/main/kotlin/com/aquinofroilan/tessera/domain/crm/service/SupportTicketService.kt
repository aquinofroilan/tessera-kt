package com.aquinofroilan.tessera.domain.crm.service

import com.aquinofroilan.tessera.domain.auth.repository.UserRepository
import com.aquinofroilan.tessera.domain.crm.dto.AddTicketMessageRequest
import com.aquinofroilan.tessera.domain.crm.dto.CreateSupportTicketRequest
import com.aquinofroilan.tessera.domain.crm.dto.SupportTicketResponse
import com.aquinofroilan.tessera.domain.crm.dto.UpdateSupportTicketRequest
import com.aquinofroilan.tessera.domain.crm.model.SupportTicket
import com.aquinofroilan.tessera.domain.crm.model.SupportTicketMessage
import com.aquinofroilan.tessera.domain.crm.model.TicketCategory
import com.aquinofroilan.tessera.domain.crm.model.TicketPriority
import com.aquinofroilan.tessera.domain.crm.model.TicketSenderType
import com.aquinofroilan.tessera.domain.crm.model.TicketStatus
import com.aquinofroilan.tessera.domain.crm.repository.ContactRepository
import com.aquinofroilan.tessera.domain.crm.repository.SupportTicketMessageRepository
import com.aquinofroilan.tessera.domain.crm.repository.SupportTicketRepository
import com.aquinofroilan.tessera.domain.sales.repository.CustomerRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@Service
class SupportTicketService(
    private val supportTicketRepository: SupportTicketRepository,
    private val supportTicketMessageRepository: SupportTicketMessageRepository,
    private val customerRepository: CustomerRepository,
    private val contactRepository: ContactRepository,
    private val userRepository: UserRepository,
) {
    @Transactional(readOnly = true)
    fun listTickets(
        organizationId: UUID,
        customerId: UUID? = null,
        status: TicketStatus? = null,
        assignedToUserId: UUID? = null,
    ): List<SupportTicketResponse> {
        val tickets =
            when {
                customerId != null && status != null ->
                    supportTicketRepository.findByOrganizationIdAndCustomerIdAndStatus(organizationId, customerId, status)
                customerId != null ->
                    supportTicketRepository.findByOrganizationIdAndCustomerId(organizationId, customerId)
                status != null ->
                    supportTicketRepository.findByOrganizationIdAndStatus(organizationId, status)
                assignedToUserId != null ->
                    supportTicketRepository.findByOrganizationIdAndAssignedToUserId(organizationId, assignedToUserId)
                else ->
                    supportTicketRepository.findByOrganizationId(organizationId)
            }

        return tickets.map { ticket ->
            val customerName = customerRepository.findById(ticket.customerId).map { it.name }.orElse(null)
            SupportTicketResponse.from(ticket, customerName = customerName, includeInternalNotes = true)
        }
    }

    @Transactional(readOnly = true)
    fun getTicket(
        id: UUID,
        organizationId: UUID,
        includeInternalNotes: Boolean = true,
    ): SupportTicketResponse {
        val ticket =
            supportTicketRepository.findByIdAndOrganizationId(id, organizationId).orElseThrow {
                ResourceNotFoundException("Support ticket $id not found")
            }
        val customerName = customerRepository.findById(ticket.customerId).map { it.name }.orElse(null)
        return SupportTicketResponse.from(ticket, customerName = customerName, includeInternalNotes = includeInternalNotes)
    }

    @Transactional
    fun createTicket(
        organizationId: UUID,
        createdByUserId: UUID,
        request: CreateSupportTicketRequest,
        senderType: TicketSenderType = TicketSenderType.AGENT,
    ): SupportTicketResponse {
        val customer =
            customerRepository.findByIdAndOrganizationId(request.customerId, organizationId).orElseThrow {
                ResourceNotFoundException("Customer ${request.customerId} not found")
            }

        if (request.contactId != null) {
            val contact =
                contactRepository.findByIdAndOrganizationId(request.contactId, organizationId).orElseThrow {
                    ResourceNotFoundException("Contact ${request.contactId} not found")
                }
            if (contact.customerId != null && contact.customerId != customer.id) {
                throw BusinessRuleException("Contact does not belong to the specified customer")
            }
        }

        if (request.assignedToUserId != null) {
            val assignee =
                userRepository.findById(request.assignedToUserId).orElseThrow {
                    ResourceNotFoundException("Assignee user ${request.assignedToUserId} not found")
                }
            if (assignee.organizationId != organizationId) {
                throw ResourceNotFoundException("Assignee user ${request.assignedToUserId} not found")
            }
        }

        val nextTicketNumber = supportTicketRepository.findMaxTicketNumberByOrganizationId(organizationId) + 1

        val ticket =
            SupportTicket(
                organizationId = organizationId,
                ticketNumber = nextTicketNumber,
                customerId = customer.id,
                contactId = request.contactId,
                subject = request.subject.trim(),
                description = request.description.trim(),
                status = TicketStatus.OPEN,
                priority = request.priority ?: TicketPriority.MEDIUM,
                category = request.category ?: TicketCategory.GENERAL_INQUIRY,
                assignedToUserId = request.assignedToUserId,
                createdByUserId = createdByUserId,
            )

        val initialMessage =
            SupportTicketMessage(
                organizationId = organizationId,
                ticketId = ticket.id,
                senderId = createdByUserId,
                senderType = senderType,
                message = request.description.trim(),
                isInternalNote = false,
            )
        ticket.messages.add(initialMessage)

        val saved = supportTicketRepository.save(ticket)
        return SupportTicketResponse.from(saved, customerName = customer.name, includeInternalNotes = true)
    }

    @Transactional
    fun updateTicket(
        id: UUID,
        organizationId: UUID,
        request: UpdateSupportTicketRequest,
    ): SupportTicketResponse {
        val ticket =
            supportTicketRepository.findByIdAndOrganizationId(id, organizationId).orElseThrow {
                ResourceNotFoundException("Support ticket $id not found")
            }

        request.subject?.let { ticket.subject = it.trim() }
        request.priority?.let { ticket.priority = it }
        request.category?.let { ticket.category = it }

        request.assignedToUserId?.let {
            val assignee =
                userRepository.findById(it).orElseThrow {
                    ResourceNotFoundException("Assignee user $it not found")
                }
            if (assignee.organizationId != organizationId) {
                throw ResourceNotFoundException("Assignee user $it not found")
            }
            ticket.assignedToUserId = it
        }

        request.status?.let { newStatus ->
            updateStatus(ticket, newStatus)
        }

        val saved = supportTicketRepository.save(ticket)
        val customerName = customerRepository.findById(saved.customerId).map { it.name }.orElse(null)
        return SupportTicketResponse.from(saved, customerName = customerName, includeInternalNotes = true)
    }

    @Transactional
    fun addMessage(
        id: UUID,
        organizationId: UUID,
        senderId: UUID,
        senderType: TicketSenderType,
        request: AddTicketMessageRequest,
    ): SupportTicketResponse {
        val ticket =
            supportTicketRepository.findByIdAndOrganizationId(id, organizationId).orElseThrow {
                ResourceNotFoundException("Support ticket $id not found")
            }

        if (ticket.status == TicketStatus.CLOSED) {
            throw BusinessRuleException("Cannot add messages to a closed support ticket")
        }

        val isInternal = request.isInternalNote ?: false
        val message =
            SupportTicketMessage(
                organizationId = organizationId,
                ticketId = ticket.id,
                senderId = senderId,
                senderType = senderType,
                message = request.message.trim(),
                isInternalNote = isInternal,
            )

        ticket.messages.add(message)
        supportTicketMessageRepository.save(message)

        // Status transition helper based on sender
        if (!isInternal) {
            if (senderType == TicketSenderType.CUSTOMER) {
                if (ticket.status == TicketStatus.WAITING_FOR_CUSTOMER || ticket.status == TicketStatus.RESOLVED) {
                    ticket.status = TicketStatus.IN_PROGRESS
                }
            } else if (senderType == TicketSenderType.AGENT) {
                if (ticket.status == TicketStatus.OPEN) {
                    ticket.status = TicketStatus.IN_PROGRESS
                }
            }
        }

        val saved = supportTicketRepository.save(ticket)
        val customerName = customerRepository.findById(saved.customerId).map { it.name }.orElse(null)
        return SupportTicketResponse.from(saved, customerName = customerName, includeInternalNotes = true)
    }

    @Transactional
    fun assignTicket(
        id: UUID,
        organizationId: UUID,
        assignedToUserId: UUID,
    ): SupportTicketResponse {
        val ticket =
            supportTicketRepository.findByIdAndOrganizationId(id, organizationId).orElseThrow {
                ResourceNotFoundException("Support ticket $id not found")
            }

        val assignee =
            userRepository.findById(assignedToUserId).orElseThrow {
                ResourceNotFoundException("Assignee user $assignedToUserId not found")
            }
        if (assignee.organizationId != organizationId) {
            throw ResourceNotFoundException("Assignee user $assignedToUserId not found")
        }

        ticket.assignedToUserId = assignedToUserId
        if (ticket.status == TicketStatus.OPEN) {
            ticket.status = TicketStatus.IN_PROGRESS
        }

        val saved = supportTicketRepository.save(ticket)
        val customerName = customerRepository.findById(saved.customerId).map { it.name }.orElse(null)
        return SupportTicketResponse.from(saved, customerName = customerName, includeInternalNotes = true)
    }

    @Transactional
    fun resolveTicket(
        id: UUID,
        organizationId: UUID,
    ): SupportTicketResponse {
        val ticket =
            supportTicketRepository.findByIdAndOrganizationId(id, organizationId).orElseThrow {
                ResourceNotFoundException("Support ticket $id not found")
            }

        updateStatus(ticket, TicketStatus.RESOLVED)
        val saved = supportTicketRepository.save(ticket)
        val customerName = customerRepository.findById(saved.customerId).map { it.name }.orElse(null)
        return SupportTicketResponse.from(saved, customerName = customerName, includeInternalNotes = true)
    }

    @Transactional
    fun closeTicket(
        id: UUID,
        organizationId: UUID,
    ): SupportTicketResponse {
        val ticket =
            supportTicketRepository.findByIdAndOrganizationId(id, organizationId).orElseThrow {
                ResourceNotFoundException("Support ticket $id not found")
            }

        updateStatus(ticket, TicketStatus.CLOSED)
        val saved = supportTicketRepository.save(ticket)
        val customerName = customerRepository.findById(saved.customerId).map { it.name }.orElse(null)
        return SupportTicketResponse.from(saved, customerName = customerName, includeInternalNotes = true)
    }

    private fun updateStatus(
        ticket: SupportTicket,
        newStatus: TicketStatus,
    ) {
        ticket.status = newStatus
        when (newStatus) {
            TicketStatus.RESOLVED -> {
                ticket.resolvedAt = LocalDateTime.now(ZoneOffset.UTC)
            }
            TicketStatus.CLOSED -> {
                if (ticket.resolvedAt == null) {
                    ticket.resolvedAt = LocalDateTime.now(ZoneOffset.UTC)
                }
                ticket.closedAt = LocalDateTime.now(ZoneOffset.UTC)
            }
            TicketStatus.OPEN, TicketStatus.IN_PROGRESS, TicketStatus.WAITING_FOR_CUSTOMER -> {
                ticket.closedAt = null
            }
        }
    }
}
