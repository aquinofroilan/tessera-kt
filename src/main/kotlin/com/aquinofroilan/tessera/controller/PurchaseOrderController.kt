package com.aquinofroilan.tessera.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.dto.BillMatchRequest
import com.aquinofroilan.tessera.dto.CreatePurchaseOrderRequest
import com.aquinofroilan.tessera.dto.GenerateBillRequest
import com.aquinofroilan.tessera.dto.PurchaseOrderResponse
import com.aquinofroilan.tessera.dto.ReceivePurchaseOrderRequest
import com.aquinofroilan.tessera.model.PurchaseOrderStatus
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.service.PurchaseOrderService
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
@RequestMapping("/procurement/purchase-orders")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class PurchaseOrderController(
    private val purchaseOrderService: PurchaseOrderService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('procurement:write')")
    fun createPurchaseOrder(
        @Valid @RequestBody request: CreatePurchaseOrderRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val createdBy = authContext.userId() ?: java.util.UUID.fromString("00000000-0000-0000-0000-000000000000")
        val po = purchaseOrderService.createPurchaseOrder(request, orgId, createdBy)
        return ResponseEntity.status(HttpStatus.CREATED).body(PurchaseOrderResponse.from(po))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('procurement:read')")
    fun listPurchaseOrders(
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) vendorId: java.util.UUID?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val poStatus =
            if (status != null) {
                try {
                    PurchaseOrderStatus.valueOf(status.uppercase(Locale.ROOT))
                } catch (e: IllegalArgumentException) {
                    return ResponseEntity.badRequest().body(mapOf("error" to "Invalid status '$status'"))
                }
            } else {
                null
            }
        val orders = purchaseOrderService.listPurchaseOrders(orgId, poStatus, vendorId)
        return ResponseEntity.ok(orders.map { PurchaseOrderResponse.from(it) })
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('procurement:read')")
    fun getPurchaseOrder(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(PurchaseOrderResponse.from(purchaseOrderService.getPurchaseOrder(id, orgId)))
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('procurement:approve')")
    fun approvePurchaseOrder(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val approvedBy = authContext.userId() ?: java.util.UUID.fromString("00000000-0000-0000-0000-000000000000")
        return ResponseEntity.ok(PurchaseOrderResponse.from(purchaseOrderService.approvePurchaseOrder(id, orgId, approvedBy)))
    }

    @PostMapping("/{id}/receive")
    @PreAuthorize("hasAuthority('procurement:receive')")
    fun receivePurchaseOrder(
        @PathVariable id: java.util.UUID,
        @Valid @RequestBody(required = false) request: ReceivePurchaseOrderRequest?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val userId = authContext.userId() ?: java.util.UUID.fromString("00000000-0000-0000-0000-000000000000")
        return ResponseEntity.ok(PurchaseOrderResponse.from(purchaseOrderService.receivePurchaseOrder(id, request, orgId, userId)))
    }

    @PostMapping("/{id}/match-bill")
    @PreAuthorize("hasAuthority('procurement:read')")
    fun matchBill(
        @PathVariable id: java.util.UUID,
        @Valid @RequestBody request: BillMatchRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(purchaseOrderService.previewBillMatch(id, request, orgId))
    }

    @PostMapping("/{id}/generate-bill")
    @PreAuthorize("hasAuthority('procurement:receive')")
    fun generateBill(
        @PathVariable id: java.util.UUID,
        @Valid @RequestBody(required = false) request: GenerateBillRequest?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val createdBy = authContext.userId() ?: java.util.UUID.fromString("00000000-0000-0000-0000-000000000000")
        val bill = purchaseOrderService.generateBill(id, request, orgId, createdBy)
        return ResponseEntity.status(HttpStatus.CREATED).body(mapOf("billId" to bill.id, "billNumber" to bill.billNumber))
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAuthority('procurement:write')")
    fun closePurchaseOrder(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(PurchaseOrderResponse.from(purchaseOrderService.closePurchaseOrder(id, orgId)))
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('procurement:write')")
    fun cancelPurchaseOrder(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val userId = authContext.userId() ?: java.util.UUID.fromString("00000000-0000-0000-0000-000000000000")
        return ResponseEntity.ok(PurchaseOrderResponse.from(purchaseOrderService.cancelPurchaseOrder(id, orgId, userId)))
    }
}
