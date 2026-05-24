package com.froilan.synectix.controller

import com.froilan.synectix.annotation.LogLevel
import com.froilan.synectix.annotation.Loggable
import com.froilan.synectix.dto.CreateReorderRuleRequest
import com.froilan.synectix.dto.ReorderRuleResponse
import com.froilan.synectix.dto.UpdateReorderRuleRequest
import com.froilan.synectix.model.InventoryReorderRule
import com.froilan.synectix.security.AuthenticationContext
import com.froilan.synectix.service.InventoryReorderRuleService
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
