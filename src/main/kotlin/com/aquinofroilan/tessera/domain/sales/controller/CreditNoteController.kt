package com.aquinofroilan.tessera.domain.sales.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.sales.dto.ApplyCreditNoteRequest
import com.aquinofroilan.tessera.domain.sales.dto.CreateCreditNoteRequest
import com.aquinofroilan.tessera.domain.sales.dto.CreditNoteResponse
import com.aquinofroilan.tessera.domain.sales.model.CreditNoteStatus
import com.aquinofroilan.tessera.domain.sales.service.CreditNoteService
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
@RequestMapping("/api/v1/sales/credit-notes")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class CreditNoteController(
    private val creditNoteService: CreditNoteService,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('sales:read') or hasAuthority('ar:read') or hasAuthority('organization:read') or hasRole('SUPER_ADMIN')")
    fun listCreditNotes(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam(required = false) customerId: UUID?,
        @RequestParam(required = false) status: CreditNoteStatus?,
    ): ResponseEntity<List<CreditNoteResponse>> = ResponseEntity.ok(creditNoteService.listCreditNotes(orgId, customerId, status))

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('sales:read') or hasAuthority('ar:read') or hasAuthority('organization:read') or hasRole('SUPER_ADMIN')")
    fun getCreditNote(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: UUID,
    ): ResponseEntity<CreditNoteResponse> = ResponseEntity.ok(creditNoteService.getCreditNote(id, orgId))

    @PostMapping
    @PreAuthorize(
        "hasAuthority('sales:create') or hasAuthority('ar:create') or hasAuthority('organization:write') or hasRole('SUPER_ADMIN')",
    )
    fun createCreditNote(
        @CurrentOrganizationId orgId: UUID,
        @CurrentUserId userId: UUID,
        @Valid @RequestBody request: CreateCreditNoteRequest,
    ): ResponseEntity<CreditNoteResponse> {
        val created = creditNoteService.createCreditNote(orgId, userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize(
        "hasAuthority('sales:create') or hasAuthority('ar:create') or hasAuthority('organization:write') or hasRole('SUPER_ADMIN')",
    )
    fun approveCreditNote(
        @CurrentOrganizationId orgId: UUID,
        @CurrentUserId userId: UUID,
        @PathVariable id: UUID,
    ): ResponseEntity<CreditNoteResponse> = ResponseEntity.ok(creditNoteService.approveCreditNote(id, orgId, userId))

    @PostMapping("/{id}/apply")
    @PreAuthorize(
        "hasAuthority('sales:create') or hasAuthority('ar:create') or hasAuthority('organization:write') or hasRole('SUPER_ADMIN')",
    )
    fun applyCreditNote(
        @CurrentOrganizationId orgId: UUID,
        @CurrentUserId userId: UUID,
        @PathVariable id: UUID,
        @Valid @RequestBody request: ApplyCreditNoteRequest,
    ): ResponseEntity<CreditNoteResponse> = ResponseEntity.ok(creditNoteService.applyCreditNoteToInvoice(id, orgId, userId, request))

    @PostMapping("/{id}/void")
    @PreAuthorize(
        "hasAuthority('sales:create') or hasAuthority('ar:create') or hasAuthority('organization:write') or hasRole('SUPER_ADMIN')",
    )
    fun voidCreditNote(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: UUID,
    ): ResponseEntity<CreditNoteResponse> = ResponseEntity.ok(creditNoteService.voidCreditNote(id, orgId))
}
