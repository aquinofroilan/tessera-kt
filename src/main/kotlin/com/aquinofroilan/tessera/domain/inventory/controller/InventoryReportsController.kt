package com.aquinofroilan.tessera.domain.inventory.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.inventory.service.InventoryReorderRuleService
import com.aquinofroilan.tessera.domain.inventory.service.InventoryReportsService
import com.aquinofroilan.tessera.domain.inventory.service.InventoryValuationService
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.security.CurrentOrganizationId
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.util.UUID

@RestController
@RequestMapping("/api/v1/inventory/reports")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class InventoryReportsController(
    private val inventoryValuationService: InventoryValuationService,
    private val inventoryReportsService: InventoryReportsService,
    private val reorderRuleService: InventoryReorderRuleService,
    private val authContext: AuthenticationContext,
) {
    @GetMapping("/valuation")
    @PreAuthorize("hasAuthority('inventory:read')")
    fun valuation(
        @CurrentOrganizationId orgId: UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(inventoryValuationService.valuation(orgId))

    @GetMapping("/stock-on-hand")
    @PreAuthorize("hasAuthority('inventory:read')")
    fun stockOnHand(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam(required = false) productId: java.util.UUID?,
        @RequestParam(required = false) warehouseId: java.util.UUID?,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        asOfDate: LocalDateTime?,
    ): ResponseEntity<Any> = ResponseEntity.ok(inventoryReportsService.stockOnHand(orgId, productId, warehouseId, asOfDate))

    @GetMapping("/movements")
    @PreAuthorize("hasAuthority('inventory:read')")
    fun movementHistory(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam(required = false) productId: java.util.UUID?,
        @RequestParam(required = false) warehouseId: java.util.UUID?,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        from: LocalDateTime?,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        to: LocalDateTime?,
    ): ResponseEntity<Any> = ResponseEntity.ok(inventoryReportsService.movementHistory(orgId, productId, warehouseId, from, to))

    @GetMapping("/low-stock")
    @PreAuthorize("hasAuthority('inventory:read')")
    fun lowStock(
        @CurrentOrganizationId orgId: UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(reorderRuleService.lowStockReport(orgId))
}
