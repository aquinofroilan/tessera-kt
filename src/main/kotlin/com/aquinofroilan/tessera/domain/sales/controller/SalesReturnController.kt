package com.aquinofroilan.tessera.domain.sales.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.sales.dto.CreateSalesReturnRequest
import com.aquinofroilan.tessera.domain.sales.dto.ReceiveSalesReturnRequest
import com.aquinofroilan.tessera.domain.sales.dto.SalesReturnResponse
import com.aquinofroilan.tessera.domain.sales.model.SalesReturnStatus
import com.aquinofroilan.tessera.domain.sales.service.SalesReturnService
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
@RequestMapping("/api/v1/sales/returns")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class SalesReturnController(
    private val salesReturnService: SalesReturnService,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('sales:read') or hasAuthority('organization:read') or hasRole('SUPER_ADMIN')")
    fun listSalesReturns(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam(required = false) customerId: UUID?,
        @RequestParam(required = false) status: SalesReturnStatus?,
    ): ResponseEntity<List<SalesReturnResponse>> = ResponseEntity.ok(salesReturnService.listSalesReturns(orgId, customerId, status))

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('sales:read') or hasAuthority('organization:read') or hasRole('SUPER_ADMIN')")
    fun getSalesReturn(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: UUID,
    ): ResponseEntity<SalesReturnResponse> = ResponseEntity.ok(salesReturnService.getSalesReturn(id, orgId))

    @PostMapping
    @PreAuthorize("hasAuthority('sales:create') or hasAuthority('organization:write') or hasRole('SUPER_ADMIN')")
    fun createSalesReturn(
        @CurrentOrganizationId orgId: UUID,
        @CurrentUserId userId: UUID,
        @Valid @RequestBody request: CreateSalesReturnRequest,
    ): ResponseEntity<SalesReturnResponse> {
        val created = salesReturnService.createSalesReturn(orgId, userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('sales:create') or hasAuthority('organization:write') or hasRole('SUPER_ADMIN')")
    fun approveSalesReturn(
        @CurrentOrganizationId orgId: UUID,
        @CurrentUserId userId: UUID,
        @PathVariable id: UUID,
    ): ResponseEntity<SalesReturnResponse> = ResponseEntity.ok(salesReturnService.approveSalesReturn(id, orgId, userId))

    @PostMapping("/{id}/receive")
    @PreAuthorize("hasAuthority('sales:create') or hasAuthority('organization:write') or hasRole('SUPER_ADMIN')")
    fun receiveSalesReturn(
        @CurrentOrganizationId orgId: UUID,
        @CurrentUserId userId: UUID,
        @PathVariable id: UUID,
        @RequestBody(required = false) request: ReceiveSalesReturnRequest?,
    ): ResponseEntity<SalesReturnResponse> = ResponseEntity.ok(salesReturnService.receiveSalesReturn(id, orgId, userId, request))

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('sales:create') or hasAuthority('organization:write') or hasRole('SUPER_ADMIN')")
    fun completeSalesReturn(
        @CurrentOrganizationId orgId: UUID,
        @CurrentUserId userId: UUID,
        @PathVariable id: UUID,
        @RequestParam(defaultValue = "true") issueCreditNote: Boolean,
    ): ResponseEntity<SalesReturnResponse> = ResponseEntity.ok(salesReturnService.completeSalesReturn(id, orgId, userId, issueCreditNote))

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('sales:create') or hasAuthority('organization:write') or hasRole('SUPER_ADMIN')")
    fun cancelSalesReturn(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: UUID,
    ): ResponseEntity<SalesReturnResponse> = ResponseEntity.ok(salesReturnService.cancelSalesReturn(id, orgId))
}
