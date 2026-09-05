package com.aquinofroilan.tessera.domain.mfg.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.mfg.dto.CreateSubcontractOrderRequest
import com.aquinofroilan.tessera.domain.mfg.dto.DispatchSubcontractOrderRequest
import com.aquinofroilan.tessera.domain.mfg.dto.ReceiveSubcontractGoodsRequest
import com.aquinofroilan.tessera.domain.mfg.dto.SubcontractOrderResponse
import com.aquinofroilan.tessera.domain.mfg.model.SubcontractOrderStatus
import com.aquinofroilan.tessera.domain.mfg.service.SubcontractOrderService
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
@RequestMapping("/api/v1/mfg/subcontract-orders")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class SubcontractOrderController(
    private val subcontractOrderService: SubcontractOrderService,
) {
    @GetMapping
    @PreAuthorize(
        "hasAuthority('mfg:read') or hasAuthority('manufacturing:read') or hasAuthority('organization:read') or hasRole('SUPER_ADMIN')",
    )
    fun list(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam(required = false) workOrderId: UUID?,
        @RequestParam(required = false) vendorId: UUID?,
        @RequestParam(required = false) status: SubcontractOrderStatus?,
    ): ResponseEntity<List<SubcontractOrderResponse>> =
        ResponseEntity.ok(subcontractOrderService.listSubcontractOrders(orgId, workOrderId, vendorId, status))

    @GetMapping("/{id}")
    @PreAuthorize(
        "hasAuthority('mfg:read') or hasAuthority('manufacturing:read') or hasAuthority('organization:read') or hasRole('SUPER_ADMIN')",
    )
    fun getById(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: UUID,
    ): ResponseEntity<SubcontractOrderResponse> = ResponseEntity.ok(subcontractOrderService.getSubcontractOrder(id, orgId))

    @PostMapping
    @PreAuthorize(
        "hasAuthority('mfg:write') or hasAuthority('manufacturing:write') or hasAuthority('organization:write') or hasRole('SUPER_ADMIN')",
    )
    fun create(
        @CurrentOrganizationId orgId: UUID,
        @CurrentUserId userId: UUID,
        @Valid @RequestBody request: CreateSubcontractOrderRequest,
    ): ResponseEntity<SubcontractOrderResponse> {
        val created = subcontractOrderService.createSubcontractOrder(orgId, userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @PostMapping("/{id}/dispatch")
    @PreAuthorize(
        "hasAuthority('mfg:write') or hasAuthority('manufacturing:write') or hasAuthority('organization:write') or hasRole('SUPER_ADMIN')",
    )
    fun dispatch(
        @CurrentOrganizationId orgId: UUID,
        @CurrentUserId userId: UUID,
        @PathVariable id: UUID,
        @Valid @RequestBody(required = false) request: DispatchSubcontractOrderRequest?,
    ): ResponseEntity<SubcontractOrderResponse> =
        ResponseEntity.ok(
            subcontractOrderService.dispatchComponents(
                id,
                orgId,
                userId,
                request ?: DispatchSubcontractOrderRequest(),
            ),
        )

    @PostMapping("/{id}/receive")
    @PreAuthorize(
        "hasAuthority('mfg:write') or hasAuthority('manufacturing:write') or hasAuthority('organization:write') or hasRole('SUPER_ADMIN')",
    )
    fun receive(
        @CurrentOrganizationId orgId: UUID,
        @CurrentUserId userId: UUID,
        @PathVariable id: UUID,
        @Valid @RequestBody request: ReceiveSubcontractGoodsRequest,
    ): ResponseEntity<SubcontractOrderResponse> =
        ResponseEntity.ok(subcontractOrderService.receiveProcessedGoods(id, orgId, userId, request))

    @PostMapping("/{id}/cancel")
    @PreAuthorize(
        "hasAuthority('mfg:write') or hasAuthority('manufacturing:write') or hasAuthority('organization:write') or hasRole('SUPER_ADMIN')",
    )
    fun cancel(
        @CurrentOrganizationId orgId: UUID,
        @CurrentUserId userId: UUID,
        @PathVariable id: UUID,
    ): ResponseEntity<SubcontractOrderResponse> = ResponseEntity.ok(subcontractOrderService.cancelSubcontractOrder(id, orgId, userId))
}
