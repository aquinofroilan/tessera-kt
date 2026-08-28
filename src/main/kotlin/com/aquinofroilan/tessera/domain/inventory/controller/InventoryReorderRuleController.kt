package com.aquinofroilan.tessera.domain.inventory.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.inventory.dto.CreateReorderRuleRequest
import com.aquinofroilan.tessera.domain.inventory.dto.ReorderRuleResponse
import com.aquinofroilan.tessera.domain.inventory.dto.UpdateReorderRuleRequest
import com.aquinofroilan.tessera.domain.inventory.model.InventoryReorderRule
import com.aquinofroilan.tessera.domain.inventory.service.InventoryReorderRuleService
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.security.CurrentOrganizationId
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/inventory/reorder-rules")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class InventoryReorderRuleController(
    private val reorderRuleService: InventoryReorderRuleService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('inventory:write')")
    fun createRule(
        @CurrentOrganizationId orgId: UUID,
        @Valid @RequestBody request: CreateReorderRuleRequest,
    ): ResponseEntity<Any> {
        val rule = reorderRuleService.createRule(request, orgId)
        return ResponseEntity.status(HttpStatus.CREATED).body(rule.toResponse())
    }

    @GetMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    fun listRules(
        @CurrentOrganizationId orgId: UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(reorderRuleService.listRules(orgId).map { it.toResponse() })

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory:read')")
    fun getRule(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(reorderRuleService.getRule(id, orgId).toResponse())

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory:write')")
    fun updateRule(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
        @Valid @RequestBody request: UpdateReorderRuleRequest,
    ): ResponseEntity<Any> = ResponseEntity.ok(reorderRuleService.updateRule(id, request, orgId).toResponse())

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory:write')")
    fun deleteRule(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        reorderRuleService.deleteRule(id, orgId)
        return ResponseEntity.noContent().build()
    }

    private fun InventoryReorderRule.toResponse() =
        ReorderRuleResponse(
            id = id,
            productId = productId,
            warehouseId = warehouseId,
            reorderPoint = reorderPoint,
            safetyStock = safetyStock,
            organizationId = organizationId,
            createdAt = createdAt?.toString(),
            updatedAt = updatedAt?.toString(),
        )
}
