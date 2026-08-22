package com.aquinofroilan.tessera.domain.mfg.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.mfg.dto.CompleteWorkOrderRequest
import com.aquinofroilan.tessera.domain.mfg.dto.CreateWorkOrderRequest
import com.aquinofroilan.tessera.domain.mfg.dto.IssueMaterialRequest
import com.aquinofroilan.tessera.domain.mfg.dto.WorkOrderResponse
import com.aquinofroilan.tessera.domain.mfg.model.WorkOrderStatus
import com.aquinofroilan.tessera.domain.mfg.service.WorkOrderExecutionService
import com.aquinofroilan.tessera.domain.mfg.service.WorkOrderService
import com.aquinofroilan.tessera.security.AuthenticationContext
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
import java.util.Locale
import java.util.UUID

@RestController
@RequestMapping("/api/v1/mfg/work-orders")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class WorkOrderController(
    private val workOrderService: WorkOrderService,
    private val workOrderExecutionService: WorkOrderExecutionService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('mfg:write')")
    fun create(
        @CurrentUserId userId: UUID,
        @CurrentOrganizationId orgId: UUID,
        @Valid @RequestBody request: CreateWorkOrderRequest,
    ): ResponseEntity<Any> {
        val wo = workOrderService.createWorkOrder(request, orgId, userId.toString())
        return ResponseEntity.status(HttpStatus.CREATED).body(WorkOrderResponse.from(wo))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('mfg:read')")
    fun list(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) productId: java.util.UUID?,
    ): ResponseEntity<Any> {
        val parsed =
            if (status != null) {
                try {
                    WorkOrderStatus.valueOf(status.uppercase(Locale.ROOT))
                } catch (e: IllegalArgumentException) {
                    return ResponseEntity.badRequest().body(mapOf("error" to "Invalid status '$status'"))
                }
            } else {
                null
            }
        val orders = workOrderService.listWorkOrders(orgId, parsed, productId)
        return ResponseEntity.ok(orders.map { WorkOrderResponse.from(it) })
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('mfg:read')")
    fun get(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(WorkOrderResponse.from(workOrderService.getWorkOrder(id, orgId)))

    @PostMapping("/{id}/release")
    @PreAuthorize("hasAuthority('mfg:approve')")
    fun release(
        @CurrentUserId userId: UUID,
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(WorkOrderResponse.from(workOrderService.releaseWorkOrder(id, orgId, userId.toString())))

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('mfg:write')")
    fun cancel(
        @CurrentUserId userId: UUID,
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(WorkOrderResponse.from(workOrderService.cancelWorkOrder(id, orgId, userId.toString())))

    @PostMapping("/{id}/issue-material")
    @PreAuthorize("hasAuthority('mfg:write')")
    fun issueMaterial(
        @CurrentUserId userId: UUID,
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
        @Valid @RequestBody request: IssueMaterialRequest,
    ): ResponseEntity<Any> {
        val wo = workOrderExecutionService.issueMaterial(id, request, orgId, userId.toString())
        return ResponseEntity.ok(WorkOrderResponse.from(wo))
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('mfg:approve')")
    fun complete(
        @CurrentUserId userId: UUID,
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
        @Valid @RequestBody request: CompleteWorkOrderRequest,
    ): ResponseEntity<Any> {
        val wo = workOrderExecutionService.completeProduction(id, request, orgId, userId.toString())
        return ResponseEntity.ok(WorkOrderResponse.from(wo))
    }
}
