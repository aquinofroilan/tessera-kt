package com.aquinofroilan.tessera.domain.crm.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.crm.dto.AddTicketMessageRequest
import com.aquinofroilan.tessera.domain.crm.dto.AssignTicketRequest
import com.aquinofroilan.tessera.domain.crm.dto.CreateSupportTicketRequest
import com.aquinofroilan.tessera.domain.crm.dto.SupportTicketResponse
import com.aquinofroilan.tessera.domain.crm.dto.UpdateSupportTicketRequest
import com.aquinofroilan.tessera.domain.crm.model.TicketSenderType
import com.aquinofroilan.tessera.domain.crm.model.TicketStatus
import com.aquinofroilan.tessera.domain.crm.service.SupportTicketService
import com.aquinofroilan.tessera.security.CurrentOrganizationId
import com.aquinofroilan.tessera.security.CurrentUserId
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/crm/tickets")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class SupportTicketController(
    private val supportTicketService: SupportTicketService,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('crm:read') or hasAuthority('sales:read') or hasAuthority('organization:read') or hasRole('SUPER_ADMIN')")
    fun listTickets(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam(required = false) customerId: UUID?,
        @RequestParam(required = false) status: TicketStatus?,
        @RequestParam(required = false) assignedToUserId: UUID?,
    ): ResponseEntity<List<SupportTicketResponse>> =
        ResponseEntity.ok(supportTicketService.listTickets(orgId, customerId, status, assignedToUserId))

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:read') or hasAuthority('sales:read') or hasAuthority('organization:read') or hasRole('SUPER_ADMIN')")
    fun getTicket(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: Long,
    ): ResponseEntity<SupportTicketResponse> = ResponseEntity.ok(supportTicketService.getTicket(id, orgId, includeInternalNotes = true))

    @PostMapping
    @PreAuthorize(
        "hasAuthority('crm:create') or hasAuthority('sales:create') or hasAuthority('organization:write') or hasRole('SUPER_ADMIN')",
    )
    fun createTicket(
        @CurrentOrganizationId orgId: UUID,
        @CurrentUserId userId: UUID,
        @Valid @RequestBody request: CreateSupportTicketRequest,
    ): ResponseEntity<SupportTicketResponse> {
        val created = supportTicketService.createTicket(orgId, userId, request, senderType = TicketSenderType.AGENT)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @PutMapping("/{id}")
    @PreAuthorize(
        "hasAuthority('crm:create') or hasAuthority('sales:create') or hasAuthority('organization:write') or hasRole('SUPER_ADMIN')",
    )
    fun updateTicket(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateSupportTicketRequest,
    ): ResponseEntity<SupportTicketResponse> = ResponseEntity.ok(supportTicketService.updateTicket(id, orgId, request))

    @PostMapping("/{id}/messages")
    @PreAuthorize(
        "hasAuthority('crm:create') or hasAuthority('sales:create') or hasAuthority('organization:write') or hasRole('SUPER_ADMIN')",
    )
    fun addMessage(
        @CurrentOrganizationId orgId: UUID,
        @CurrentUserId userId: UUID,
        @PathVariable id: Long,
        @Valid @RequestBody request: AddTicketMessageRequest,
    ): ResponseEntity<SupportTicketResponse> =
        ResponseEntity.ok(supportTicketService.addMessage(id, orgId, userId, TicketSenderType.AGENT, request))

    @PostMapping("/{id}/assign")
    @PreAuthorize(
        "hasAuthority('crm:create') or hasAuthority('sales:create') or hasAuthority('organization:write') or hasRole('SUPER_ADMIN')",
    )
    fun assignTicket(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: Long,
        @Valid @RequestBody request: AssignTicketRequest,
    ): ResponseEntity<SupportTicketResponse> = ResponseEntity.ok(supportTicketService.assignTicket(id, orgId, request.assignedToUserId))

    @PostMapping("/{id}/resolve")
    @PreAuthorize(
        "hasAuthority('crm:create') or hasAuthority('sales:create') or hasAuthority('organization:write') or hasRole('SUPER_ADMIN')",
    )
    fun resolveTicket(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: Long,
    ): ResponseEntity<SupportTicketResponse> = ResponseEntity.ok(supportTicketService.resolveTicket(id, orgId))

    @PostMapping("/{id}/close")
    @PreAuthorize(
        "hasAuthority('crm:create') or hasAuthority('sales:create') or hasAuthority('organization:write') or hasRole('SUPER_ADMIN')",
    )
    fun closeTicket(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: Long,
    ): ResponseEntity<SupportTicketResponse> = ResponseEntity.ok(supportTicketService.closeTicket(id, orgId))
}
