package com.aquinofroilan.tessera.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.dto.CreateSalesOrderRequest
import com.aquinofroilan.tessera.dto.SalesOrderResponse
import com.aquinofroilan.tessera.model.SalesOrderStatus
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.service.SalesOrderService
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
@RequestMapping("/sales/sales-orders")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class SalesOrderController(
    private val salesOrderService: SalesOrderService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('sales:write')")
    fun createSalesOrder(
        @Valid @RequestBody request: CreateSalesOrderRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val createdBy = authContext.userId() ?: "api-key"
        val so = salesOrderService.createSalesOrder(request, orgId, createdBy)
        return ResponseEntity.status(HttpStatus.CREATED).body(SalesOrderResponse.from(so))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('sales:read')")
    fun listSalesOrders(
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) customerId: String?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val soStatus =
            if (status != null) {
                try {
                    SalesOrderStatus.valueOf(status.uppercase(Locale.ROOT))
                } catch (e: IllegalArgumentException) {
                    return ResponseEntity.badRequest().body(mapOf("error" to "Invalid status '$status'"))
                }
            } else {
                null
            }
        val orders = salesOrderService.listSalesOrders(orgId, soStatus, customerId)
        return ResponseEntity.ok(orders.map { SalesOrderResponse.from(it) })
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('sales:read')")
    fun getSalesOrder(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(SalesOrderResponse.from(salesOrderService.getSalesOrder(id, orgId)))
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('sales:approve')")
    fun approveSalesOrder(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val approvedBy = authContext.userId() ?: "api-key"
        return ResponseEntity.ok(SalesOrderResponse.from(salesOrderService.approveSalesOrder(id, orgId, approvedBy)))
    }

    @PostMapping("/{id}/fulfill")
    @PreAuthorize("hasAuthority('sales:fulfill')")
    fun fulfillSalesOrder(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val userId = authContext.userId() ?: "api-key"
        return ResponseEntity.ok(SalesOrderResponse.from(salesOrderService.fulfillSalesOrder(id, orgId, userId)))
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAuthority('sales:write')")
    fun closeSalesOrder(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(SalesOrderResponse.from(salesOrderService.closeSalesOrder(id, orgId)))
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('sales:write')")
    fun cancelSalesOrder(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(SalesOrderResponse.from(salesOrderService.cancelSalesOrder(id, orgId)))
    }
}
