package com.aquinofroilan.tessera.domain.procurement.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.procurement.dto.ConvertPurchaseRequestRequest
import com.aquinofroilan.tessera.domain.procurement.dto.CreatePurchaseRequestRequest
import com.aquinofroilan.tessera.domain.procurement.dto.PurchaseOrderResponse
import com.aquinofroilan.tessera.domain.procurement.dto.PurchaseRequestResponse
import com.aquinofroilan.tessera.domain.procurement.dto.RejectPurchaseRequestRequest
import com.aquinofroilan.tessera.domain.procurement.model.PurchaseRequestStatus
import com.aquinofroilan.tessera.domain.procurement.service.PurchaseRequestService
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.security.CurrentOrganizationId
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
import java.util.Locale
import java.util.UUID

@RestController
@RequestMapping("/api/v1/procurement/purchase-requests")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class PurchaseRequestController(
    private val purchaseRequestService: PurchaseRequestService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('procurement:write')")
    fun createPurchaseRequest(
        @CurrentOrganizationId orgId: UUID,
        @Valid @RequestBody request: CreatePurchaseRequestRequest,
    ): ResponseEntity<Any> {
        val requestedBy = authContext.userId() ?: java.util.UUID.fromString("00000000-0000-0000-0000-000000000000")
        val created = purchaseRequestService.createPurchaseRequest(request, orgId, requestedBy)
        return ResponseEntity.status(HttpStatus.CREATED).body(PurchaseRequestResponse.from(created))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('procurement:read')")
    fun listPurchaseRequests(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) requestedBy: java.util.UUID?,
    ): ResponseEntity<Any> {
        val prStatus =
            if (status != null) {
                try {
                    PurchaseRequestStatus.valueOf(status.uppercase(Locale.ROOT))
                } catch (e: IllegalArgumentException) {
                    return ResponseEntity.badRequest().body(mapOf("error" to "Invalid status '$status'"))
                }
            } else {
                null
            }
        return ResponseEntity.ok(
            purchaseRequestService.listPurchaseRequests(orgId, prStatus, requestedBy).map { PurchaseRequestResponse.from(it) },
        )
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('procurement:read')")
    fun getPurchaseRequest(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(PurchaseRequestResponse.from(purchaseRequestService.getPurchaseRequest(id, orgId)))

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('procurement:write')")
    fun submitPurchaseRequest(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(PurchaseRequestResponse.from(purchaseRequestService.submitPurchaseRequest(id, orgId)))

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('procurement:approve')")
    fun approvePurchaseRequest(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val decidedBy = authContext.userId() ?: java.util.UUID.fromString("00000000-0000-0000-0000-000000000000")
        return ResponseEntity.ok(PurchaseRequestResponse.from(purchaseRequestService.approvePurchaseRequest(id, orgId, decidedBy)))
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('procurement:approve')")
    fun rejectPurchaseRequest(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
        @RequestBody(required = false) request: RejectPurchaseRequestRequest?,
    ): ResponseEntity<Any> {
        val decidedBy = authContext.userId() ?: java.util.UUID.fromString("00000000-0000-0000-0000-000000000000")
        return ResponseEntity.ok(
            PurchaseRequestResponse.from(purchaseRequestService.rejectPurchaseRequest(id, request?.reason, orgId, decidedBy)),
        )
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('procurement:write')")
    fun cancelPurchaseRequest(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(PurchaseRequestResponse.from(purchaseRequestService.cancelPurchaseRequest(id, orgId)))

    @PostMapping("/{id}/convert")
    @PreAuthorize("hasAuthority('procurement:write')")
    fun convertPurchaseRequest(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
        @Valid @RequestBody(required = false) request: ConvertPurchaseRequestRequest?,
    ): ResponseEntity<Any> {
        val createdBy = authContext.userId() ?: java.util.UUID.fromString("00000000-0000-0000-0000-000000000000")
        val po =
            purchaseRequestService.convertToPurchaseOrder(
                id,
                request ?: ConvertPurchaseRequestRequest(),
                orgId,
                createdBy,
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(PurchaseOrderResponse.from(po))
    }
}
