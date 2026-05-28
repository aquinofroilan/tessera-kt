package com.loom.synectix.controller

import com.loom.synectix.annotation.LogLevel
import com.loom.synectix.annotation.Loggable
import com.loom.synectix.dto.CreateReorderRuleRequest
import com.loom.synectix.dto.ReorderRuleResponse
import com.loom.synectix.dto.UpdateReorderRuleRequest
import com.loom.synectix.model.InventoryReorderRule
import com.loom.synectix.security.AuthenticationContext
import com.loom.synectix.service.InventoryReorderRuleService
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

@RestController
@RequestMapping("/inventory/reorder-rules")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class InventoryReorderRuleController(
    private val reorderRuleService: InventoryReorderRuleService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('inventory:write')")
    fun createRule(
        @Valid @RequestBody request: CreateReorderRuleRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val rule = reorderRuleService.createRule(request, orgId)
        return ResponseEntity.status(HttpStatus.CREATED).body(rule.toResponse())
    }

    @GetMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    fun listRules(): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(reorderRuleService.listRules(orgId).map { it.toResponse() })
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory:read')")
    fun getRule(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(reorderRuleService.getRule(id, orgId).toResponse())
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory:write')")
    fun updateRule(
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateReorderRuleRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(reorderRuleService.updateRule(id, request, orgId).toResponse())
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory:write')")
    fun deleteRule(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
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
