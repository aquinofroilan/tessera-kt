package com.froilan.synectix.controller

import com.froilan.synectix.annotation.LogLevel
import com.froilan.synectix.annotation.Loggable
import com.froilan.synectix.security.AuthenticationContext
import com.froilan.synectix.service.InventoryValuationService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/inventory/reports")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class InventoryValuationController(
    private val inventoryValuationService: InventoryValuationService,
    private val authContext: AuthenticationContext,
) {
    @GetMapping("/valuation")
    @PreAuthorize("hasAuthority('inventory:read')")
    fun valuation(): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(inventoryValuationService.valuation(orgId))
    }
}
