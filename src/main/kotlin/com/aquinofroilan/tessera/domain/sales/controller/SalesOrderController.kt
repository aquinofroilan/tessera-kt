package com.aquinofroilan.tessera.domain.sales.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.sales.dto.CreateSalesOrderRequest
import com.aquinofroilan.tessera.domain.sales.dto.FulfillSalesOrderRequest
import com.aquinofroilan.tessera.domain.sales.dto.GenerateInvoiceRequest
import com.aquinofroilan.tessera.domain.sales.dto.SalesOrderResponse
import com.aquinofroilan.tessera.domain.sales.model.SalesOrderStatus
import com.aquinofroilan.tessera.domain.sales.service.SalesOrderService
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
@RequestMapping("/api/v1/sales/sales-orders")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class SalesOrderController(
    private val salesOrderService: SalesOrderService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('sales:write')")
    fun createSalesOrder(
        @CurrentOrganizationId orgId: UUID,
        @CurrentUserId userId: UUID,
        @Valid @RequestBody request: CreateSalesOrderRequest,
    ): ResponseEntity<Any> {
        val so = salesOrderService.createSalesOrder(request, orgId, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(SalesOrderResponse.from(so))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('sales:read')")
    fun listSalesOrders(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) customerId: java.util.UUID?,
    ): ResponseEntity<Any> {
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
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(SalesOrderResponse.from(salesOrderService.getSalesOrder(id, orgId)))

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('sales:approve')")
    fun approveSalesOrder(
        @CurrentOrganizationId orgId: UUID,
        @CurrentUserId userId: UUID,
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(SalesOrderResponse.from(salesOrderService.approveSalesOrder(id, orgId, userId)))

    @PostMapping("/{id}/fulfill")
    @PreAuthorize("hasAuthority('sales:fulfill')")
    fun fulfillSalesOrder(
        @CurrentUserId userId: UUID,
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
        @Valid @RequestBody(required = false) request: FulfillSalesOrderRequest?,
    ): ResponseEntity<Any> = ResponseEntity.ok(SalesOrderResponse.from(salesOrderService.fulfillSalesOrder(id, request, orgId, userId)))

    @PostMapping("/{id}/generate-invoice")
    @PreAuthorize("hasAuthority('sales:fulfill')")
    fun generateInvoice(
        @CurrentOrganizationId orgId: UUID,
        @CurrentUserId userId: UUID,
        @PathVariable id: java.util.UUID,
        @Valid @RequestBody(required = false) request: GenerateInvoiceRequest?,
    ): ResponseEntity<Any> {
        val invoice = salesOrderService.generateInvoice(id, request, orgId, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(mapOf("invoiceId" to invoice.id, "invoiceNumber" to invoice.invoiceNumber))
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAuthority('sales:write')")
    fun closeSalesOrder(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(SalesOrderResponse.from(salesOrderService.closeSalesOrder(id, orgId)))

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('sales:write')")
    fun cancelSalesOrder(
        @CurrentUserId userId: UUID,
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(SalesOrderResponse.from(salesOrderService.cancelSalesOrder(id, orgId, userId)))
}
