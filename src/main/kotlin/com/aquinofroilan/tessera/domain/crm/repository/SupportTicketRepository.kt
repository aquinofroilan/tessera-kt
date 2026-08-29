package com.aquinofroilan.tessera.domain.crm.repository

import com.aquinofroilan.tessera.domain.crm.model.SupportTicket
import com.aquinofroilan.tessera.domain.crm.model.TicketStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.springframework.data.jpa.repository.Query
import java.util.Optional
import java.util.UUID

@Repository
interface SupportTicketRepository : JpaRepository<SupportTicket, UUID> {
    fun findByOrganizationId(organizationId: UUID): List<SupportTicket>

    fun findByIdAndOrganizationId(
        id: UUID,
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

    @Query(
        "SELECT COALESCE(MAX(t.ticketNumber), 0) FROM SupportTicket t WHERE t.organizationId = :organizationId",
    )
    fun findMaxTicketNumberByOrganizationId(organizationId: UUID): Int

    fun findByOrganizationIdAndTicketNumber(
        organizationId: UUID,
        ticketNumber: Int,
    ): Optional<SupportTicket>
}
