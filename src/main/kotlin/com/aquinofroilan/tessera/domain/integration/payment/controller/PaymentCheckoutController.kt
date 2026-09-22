package com.aquinofroilan.tessera.domain.integration.payment.controller

import com.aquinofroilan.tessera.domain.integration.payment.dto.PaymentCheckoutRequest
import com.aquinofroilan.tessera.domain.integration.payment.dto.PaymentCheckoutResponse
import com.aquinofroilan.tessera.domain.integration.payment.service.PaymentOrchestrator
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/integrations/payments/checkout")
class PaymentCheckoutController(
    private val paymentOrchestrator: PaymentOrchestrator,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('finance:write')")
    fun createCheckoutSession(
        @Valid @RequestBody request: PaymentCheckoutRequest,
        @RequestParam(defaultValue = "https://tessera.local/success") successUrl: String,
        @RequestParam(defaultValue = "https://tessera.local/cancel") cancelUrl: String,
    ): ResponseEntity<PaymentCheckoutResponse> {
        val response = paymentOrchestrator.createCheckoutSession(request, successUrl, cancelUrl)
        return ResponseEntity.ok(response)
    }
}
