package com.aquinofroilan.tessera.domain.crm.repository

import com.aquinofroilan.tessera.domain.crm.model.SupportTicketMessage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SupportTicketMessageRepository : JpaRepository<SupportTicketMessage, UUID> {
    fun findByTicketIdOrderByCreatedAtAsc(ticketId: UUID): List<SupportTicketMessage>

    fun findByTicketIdAndIsInternalNoteFalseOrderByCreatedAtAsc(ticketId: UUID): List<SupportTicketMessage>
}
