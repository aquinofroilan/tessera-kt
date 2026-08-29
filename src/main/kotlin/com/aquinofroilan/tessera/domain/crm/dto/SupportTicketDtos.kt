package com.aquinofroilan.tessera.domain.crm.dto

import com.aquinofroilan.tessera.domain.crm.model.SupportTicket
import com.aquinofroilan.tessera.domain.crm.model.SupportTicketMessage
import com.aquinofroilan.tessera.domain.crm.model.TicketCategory
import com.aquinofroilan.tessera.domain.crm.model.TicketPriority
import com.aquinofroilan.tessera.domain.crm.model.TicketSenderType
import com.aquinofroilan.tessera.domain.crm.model.TicketStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime
import java.util.UUID

data class SupportTicketMessageDto(
    val id: UUID,
    val ticketId: Long,
    val senderId: UUID,
    val senderType: TicketSenderType,
    val message: String,
    val isInternalNote: Boolean,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(msg: SupportTicketMessage): SupportTicketMessageDto =
            SupportTicketMessageDto(
                id = msg.id,
                ticketId = msg.ticketId,
                senderId = msg.senderId,
                senderType = msg.senderType,
                message = msg.message,
                isInternalNote = msg.isInternalNote,
                createdAt = msg.createdAt,
            )
    }
}

data class SupportTicketResponse(
    val id: Long,
    val organizationId: UUID,
    val customerId: UUID,
    val customerName: String?,
    val contactId: UUID?,
    val subject: String,
    val description: String,
    val status: TicketStatus,
    val priority: TicketPriority,
    val category: TicketCategory,
    val assignedToUserId: UUID?,
    val createdByUserId: UUID,
    val resolvedAt: LocalDateTime?,
    val closedAt: LocalDateTime?,
    val messages: List<SupportTicketMessageDto>,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
) {
    companion object {
        fun from(
            ticket: SupportTicket,
            customerName: String? = null,
            includeInternalNotes: Boolean = true,
        ): SupportTicketResponse {
            val msgs =
                if (includeInternalNotes) {
                    ticket.messages
                } else {
                    ticket.messages.filter { !it.isInternalNote }
                }
            return SupportTicketResponse(
                id = ticket.id,
                organizationId = ticket.organizationId,
                customerId = ticket.customerId,
                customerName = customerName,
                contactId = ticket.contactId,
                subject = ticket.subject,
                description = ticket.description,
                status = ticket.status,
                priority = ticket.priority,
                category = ticket.category,
                assignedToUserId = ticket.assignedToUserId,
                createdByUserId = ticket.createdByUserId,
                resolvedAt = ticket.resolvedAt,
                closedAt = ticket.closedAt,
                messages = msgs.map { SupportTicketMessageDto.from(it) },
                createdAt = ticket.createdAt,
                updatedAt = ticket.updatedAt,
            )
        }
    }
}

data class CreateSupportTicketRequest(
    @field:NotNull(message = "Customer ID is required")
    val customerId: UUID,
    val contactId: UUID? = null,
    @field:NotBlank(message = "Subject is required")
    val subject: String,
    @field:NotBlank(message = "Description is required")
    val description: String,
    val priority: TicketPriority? = null,
    val category: TicketCategory? = null,
    val assignedToUserId: UUID? = null,
)

data class UpdateSupportTicketRequest(
    val subject: String? = null,
    val status: TicketStatus? = null,
    val priority: TicketPriority? = null,
    val category: TicketCategory? = null,
    val assignedToUserId: UUID? = null,
)

data class AddTicketMessageRequest(
    @field:NotBlank(message = "Message is required")
    val message: String,
    val isInternalNote: Boolean? = null,
)

data class AssignTicketRequest(
    @field:NotNull(message = "Assigned user ID is required")
    val assignedToUserId: UUID,
)
