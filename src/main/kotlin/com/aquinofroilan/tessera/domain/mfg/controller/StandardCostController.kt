package com.aquinofroilan.tessera.domain.mfg.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.mfg.dto.ManualStandardCostRequest
import com.aquinofroilan.tessera.domain.mfg.dto.RollupRequest
import com.aquinofroilan.tessera.domain.mfg.dto.StandardCostResponse
import com.aquinofroilan.tessera.domain.mfg.service.StandardCostService
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.security.CurrentOrganizationId
import com.aquinofroilan.tessera.security.CurrentUserId
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
import java.util.UUID

@RestController
@RequestMapping("/api/v1/mfg/products")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class StandardCostController(
    private val standardCostService: StandardCostService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping("/{productId}/cost-rollup")
    @PreAuthorize("hasAuthority('mfg:approve')")
    fun rollup(
        @CurrentUserId userId: UUID,
        @CurrentOrganizationId orgId: UUID,
        @PathVariable productId: java.util.UUID,
        @Valid @RequestBody(required = false) request: RollupRequest?,
    ): ResponseEntity<Any> {
        val record = standardCostService.rollup(productId, request ?: RollupRequest(), orgId, userId.toString())
        return ResponseEntity.ok(StandardCostResponse.from(record))
    }

    @PutMapping("/{productId}/standard-cost")
    @PreAuthorize("hasAuthority('mfg:approve')")
    fun setManual(
        @CurrentUserId userId: UUID,
        @CurrentOrganizationId orgId: UUID,
        @PathVariable productId: java.util.UUID,
        @Valid @RequestBody request: ManualStandardCostRequest,
    ): ResponseEntity<Any> {
        val record = standardCostService.setManual(productId, request, orgId, userId.toString())
        return ResponseEntity.ok(StandardCostResponse.from(record))
    }

    @GetMapping("/{productId}/standard-cost")
    @PreAuthorize("hasAuthority('mfg:read')")
    fun get(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable productId: java.util.UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(StandardCostResponse.from(standardCostService.getStandardCost(productId, orgId)))

    @GetMapping("/standard-costs")
    @PreAuthorize("hasAuthority('mfg:read')")
    fun list(
        @CurrentOrganizationId orgId: UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(standardCostService.listStandardCosts(orgId).map { StandardCostResponse.from(it) })
}
