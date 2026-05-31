package com.aquinofroilan.tessera.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.dto.ManualStandardCostRequest
import com.aquinofroilan.tessera.dto.RollupRequest
import com.aquinofroilan.tessera.dto.StandardCostResponse
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.service.StandardCostService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/mfg/products")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class StandardCostController(
    private val standardCostService: StandardCostService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping("/{productId}/cost-rollup")
    @PreAuthorize("hasAuthority('mfg:approve')")
    fun rollup(
        @PathVariable productId: String,
        @Valid @RequestBody(required = false) request: RollupRequest?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val userId = authContext.userId() ?: "api-key"
        val record = standardCostService.rollup(productId, request ?: RollupRequest(), orgId, userId)
        return ResponseEntity.ok(StandardCostResponse.from(record))
    }

    @PutMapping("/{productId}/standard-cost")
    @PreAuthorize("hasAuthority('mfg:approve')")
    fun setManual(
        @PathVariable productId: String,
        @Valid @RequestBody request: ManualStandardCostRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val userId = authContext.userId() ?: "api-key"
        val record = standardCostService.setManual(productId, request, orgId, userId)
        return ResponseEntity.ok(StandardCostResponse.from(record))
    }

    @GetMapping("/{productId}/standard-cost")
    @PreAuthorize("hasAuthority('mfg:read')")
    fun get(
        @PathVariable productId: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(StandardCostResponse.from(standardCostService.getStandardCost(productId, orgId)))
    }

    @GetMapping("/standard-costs")
    @PreAuthorize("hasAuthority('mfg:read')")
    fun list(): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(standardCostService.listStandardCosts(orgId).map { StandardCostResponse.from(it) })
    }
}
