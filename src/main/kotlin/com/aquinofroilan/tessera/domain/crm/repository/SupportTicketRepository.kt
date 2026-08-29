package com.aquinofroilan.tessera.domain.crm.repository

import com.aquinofroilan.tessera.domain.crm.model.SupportTicket
import com.aquinofroilan.tessera.domain.crm.model.TicketStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface SupportTicketRepository : JpaRepository<SupportTicket, Long> {
    fun findByOrganizationId(organizationId: UUID): List<SupportTicket>

    fun findByIdAndOrganizationId(
        id: Long,
        organizationId: UUID,
    ): Optional<SupportTicket>

    fun findByOrganizationIdAndCustomerId(
        organizationId: UUID,
        customerId: UUID,
    ): List<SupportTicket>

    fun findByOrganizationIdAndCustomerIdAndStatus(
        organizationId: UUID,
        customerId: UUID,
        status: TicketStatus,
    ): List<SupportTicket>

    fun findByOrganizationIdAndStatus(
        organizationId: UUID,
        status: TicketStatus,
    ): List<SupportTicket>

    fun findByOrganizationIdAndAssignedToUserId(
        organizationId: UUID,
        userId: UUID,
    ): List<SupportTicket>

    fun countByOrganizationId(organizationId: UUID): Long

    fun countByOrganizationIdAndCustomerIdAndStatusIn(
        organizationId: UUID,
        customerId: UUID,
        statuses: List<TicketStatus>,
    ): Long
}
