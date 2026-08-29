package com.aquinofroilan.tessera.domain.crm.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.crm.dto.AddTicketMessageRequest
import com.aquinofroilan.tessera.domain.crm.dto.CreatePortalTicketRequest
import com.aquinofroilan.tessera.domain.crm.dto.CustomerPortalSummaryResponse
import com.aquinofroilan.tessera.domain.crm.dto.PortalInvoiceResponse
import com.aquinofroilan.tessera.domain.crm.dto.PortalOrderResponse
import com.aquinofroilan.tessera.domain.crm.dto.SupportTicketResponse
import com.aquinofroilan.tessera.domain.crm.model.TicketStatus
import com.aquinofroilan.tessera.domain.crm.service.CustomerPortalService
import com.aquinofroilan.tessera.domain.finance.model.InvoiceStatus
import com.aquinofroilan.tessera.domain.sales.model.SalesOrderStatus
import com.aquinofroilan.tessera.security.CurrentOrganizationId
import com.aquinofroilan.tessera.security.CurrentUserId
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/portal")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class CustomerPortalController(
    private val customerPortalService: CustomerPortalService,
) {
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    fun getMySummary(
        @CurrentOrganizationId orgId: UUID,
        @CurrentUserId userId: UUID,
    ): ResponseEntity<CustomerPortalSummaryResponse> = ResponseEntity.ok(customerPortalService.getMyPortalSummary(orgId, userId))

    // -------------------------------------------------------------------------
    // Invoices
    // -------------------------------------------------------------------------
    @GetMapping("/invoices")
    @PreAuthorize("isAuthenticated()")
    fun getMyInvoices(
        @CurrentOrganizationId orgId: UUID,
        @CurrentUserId userId: UUID,
        @RequestParam(required = false) status: InvoiceStatus?,
    ): ResponseEntity<List<PortalInvoiceResponse>> = ResponseEntity.ok(customerPortalService.getMyInvoices(orgId, userId, status))

    @GetMapping("/invoices/{id}")
    @PreAuthorize("isAuthenticated()")
    fun getMyInvoice(
        @CurrentOrganizationId orgId: UUID,
        @CurrentUserId userId: UUID,
        @PathVariable id: UUID,
    ): ResponseEntity<PortalInvoiceResponse> = ResponseEntity.ok(customerPortalService.getMyInvoice(orgId, userId, id))

    // -------------------------------------------------------------------------
    // Orders
    // -------------------------------------------------------------------------
    @GetMapping("/orders")
    @PreAuthorize("isAuthenticated()")
    fun getMyOrders(
        @CurrentOrganizationId orgId: UUID,
        @CurrentUserId userId: UUID,
        @RequestParam(required = false) status: SalesOrderStatus?,
    ): ResponseEntity<List<PortalOrderResponse>> = ResponseEntity.ok(customerPortalService.getMyOrders(orgId, userId, status))

    @GetMapping("/orders/{id}")
    @PreAuthorize("isAuthenticated()")
    fun getMyOrder(
        @CurrentOrganizationId orgId: UUID,
        @CurrentUserId userId: UUID,
        @PathVariable id: UUID,
    ): ResponseEntity<PortalOrderResponse> = ResponseEntity.ok(customerPortalService.getMyOrder(orgId, userId, id))

    // -------------------------------------------------------------------------
    // Support Tickets
    // -------------------------------------------------------------------------
    @GetMapping("/tickets")
    @PreAuthorize("isAuthenticated()")
    fun getMyTickets(
        @CurrentOrganizationId orgId: UUID,
        @CurrentUserId userId: UUID,
        @RequestParam(required = false) status: TicketStatus?,
    ): ResponseEntity<List<SupportTicketResponse>> = ResponseEntity.ok(customerPortalService.getMyTickets(orgId, userId, status))

    @GetMapping("/tickets/{id}")
    @PreAuthorize("isAuthenticated()")
    fun getMyTicket(
        @CurrentOrganizationId orgId: UUID,
        @CurrentUserId userId: UUID,
        @PathVariable id: UUID,
    ): ResponseEntity<SupportTicketResponse> = ResponseEntity.ok(customerPortalService.getMyTicket(orgId, userId, id))

    @PostMapping("/tickets")
    @PreAuthorize("isAuthenticated()")
    fun createMyTicket(
        @CurrentOrganizationId orgId: UUID,
        @CurrentUserId userId: UUID,
        @Valid @RequestBody request: CreatePortalTicketRequest,
    ): ResponseEntity<SupportTicketResponse> {
        val created = customerPortalService.createMyTicket(orgId, userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @PostMapping("/tickets/{id}/messages")
    @PreAuthorize("isAuthenticated()")
    fun addMyTicketMessage(
        @CurrentOrganizationId orgId: UUID,
        @CurrentUserId userId: UUID,
        @PathVariable id: UUID,
        @Valid @RequestBody request: AddTicketMessageRequest,
    ): ResponseEntity<SupportTicketResponse> =
        ResponseEntity.ok(customerPortalService.addMyTicketMessage(orgId, userId, id, request.message))

    @PostMapping("/tickets/{id}/close")
    @PreAuthorize("isAuthenticated()")
    fun closeMyTicket(
        @CurrentOrganizationId orgId: UUID,
        @CurrentUserId userId: UUID,
        @PathVariable id: UUID,
    ): ResponseEntity<SupportTicketResponse> = ResponseEntity.ok(customerPortalService.closeMyTicket(orgId, userId, id))
}
