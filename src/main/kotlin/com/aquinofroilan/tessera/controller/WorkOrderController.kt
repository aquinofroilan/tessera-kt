package com.aquinofroilan.tessera.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.dto.CreateWorkOrderRequest
import com.aquinofroilan.tessera.dto.WorkOrderResponse
import com.aquinofroilan.tessera.model.WorkOrderStatus
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.service.WorkOrderService
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

@RestController
@RequestMapping("/mfg/work-orders")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class WorkOrderController(
    private val workOrderService: WorkOrderService,
    private val workOrderExecutionService: WorkOrderExecutionService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('mfg:write')")
    fun create(
        @Valid @RequestBody request: CreateWorkOrderRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId()?.toString() ?: return authContext.unauthorized()
        val userId = authContext.userId()?.toString() ?: "api-key"
        val wo = workOrderService.createWorkOrder(request, orgId, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(WorkOrderResponse.from(wo))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('mfg:read')")
    fun list(
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) productId: String?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId()?.toString() ?: return authContext.unauthorized()
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
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId()?.toString() ?: return authContext.unauthorized()
        return ResponseEntity.ok(WorkOrderResponse.from(workOrderService.getWorkOrder(id, orgId)))
    }

    @PostMapping("/{id}/release")
    @PreAuthorize("hasAuthority('mfg:approve')")
    fun release(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId()?.toString() ?: return authContext.unauthorized()
        val userId = authContext.userId()?.toString() ?: "api-key"
        return ResponseEntity.ok(WorkOrderResponse.from(workOrderService.releaseWorkOrder(id, orgId, userId)))
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('mfg:write')")
    fun cancel(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId()?.toString() ?: return authContext.unauthorized()
        val userId = authContext.userId()?.toString() ?: "api-key"
        return ResponseEntity.ok(WorkOrderResponse.from(workOrderService.cancelWorkOrder(id, orgId, userId)))
    }
}
