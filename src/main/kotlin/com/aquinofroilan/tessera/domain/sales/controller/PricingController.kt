package com.aquinofroilan.tessera.domain.sales.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.sales.dto.CalculatePriceRequest
import com.aquinofroilan.tessera.domain.sales.dto.CalculatePriceResponse
import com.aquinofroilan.tessera.domain.sales.service.PricingCalculationService
import com.aquinofroilan.tessera.security.CurrentOrganizationId
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/sales/pricing")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class PricingController(
    private val pricingCalculationService: PricingCalculationService,
) {
    @PostMapping("/calculate")
    @PreAuthorize("hasAuthority('sales:read') or hasAuthority('organization:read') or hasRole('SUPER_ADMIN')")
    fun calculatePrice(
        @CurrentOrganizationId orgId: UUID,
        @Valid @RequestBody request: CalculatePriceRequest,
    ): ResponseEntity<CalculatePriceResponse> = ResponseEntity.ok(pricingCalculationService.calculatePrice(orgId, request))
}
