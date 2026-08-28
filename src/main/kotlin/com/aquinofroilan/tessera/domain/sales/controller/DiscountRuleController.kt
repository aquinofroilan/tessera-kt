package com.aquinofroilan.tessera.domain.sales.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.sales.dto.CreateDiscountRuleRequest
import com.aquinofroilan.tessera.domain.sales.dto.DiscountRuleResponse
import com.aquinofroilan.tessera.domain.sales.dto.UpdateDiscountRuleRequest
import com.aquinofroilan.tessera.domain.sales.service.DiscountRuleService
import com.aquinofroilan.tessera.security.CurrentOrganizationId
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/sales/discount-rules")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class DiscountRuleController(
    private val discountRuleService: DiscountRuleService,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('sales:read') or hasAuthority('organization:read') or hasRole('SUPER_ADMIN')")
    fun listDiscountRules(
        @CurrentOrganizationId orgId: UUID,
    ): ResponseEntity<List<DiscountRuleResponse>> = ResponseEntity.ok(discountRuleService.listDiscountRules(orgId))

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('sales:read') or hasAuthority('organization:read') or hasRole('SUPER_ADMIN')")
    fun getDiscountRule(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: UUID,
    ): ResponseEntity<DiscountRuleResponse> = ResponseEntity.ok(discountRuleService.getDiscountRule(id, orgId))

    @PostMapping
    @PreAuthorize("hasAuthority('sales:create') or hasAuthority('organization:write') or hasRole('SUPER_ADMIN')")
    fun createDiscountRule(
        @CurrentOrganizationId orgId: UUID,
        @Valid @RequestBody request: CreateDiscountRuleRequest,
    ): ResponseEntity<DiscountRuleResponse> {
        val created = discountRuleService.createDiscountRule(orgId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('sales:create') or hasAuthority('organization:write') or hasRole('SUPER_ADMIN')")
    fun updateDiscountRule(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateDiscountRuleRequest,
    ): ResponseEntity<DiscountRuleResponse> = ResponseEntity.ok(discountRuleService.updateDiscountRule(id, orgId, request))

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('sales:create') or hasAuthority('organization:write') or hasRole('SUPER_ADMIN')")
    fun deleteDiscountRule(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        discountRuleService.deleteDiscountRule(id, orgId)
        return ResponseEntity.noContent().build()
    }
}
