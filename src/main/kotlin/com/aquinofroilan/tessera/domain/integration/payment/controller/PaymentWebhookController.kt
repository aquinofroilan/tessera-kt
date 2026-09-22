package com.aquinofroilan.tessera.domain.integration.payment.controller

import com.aquinofroilan.tessera.domain.integration.payment.model.GatewayType
import com.aquinofroilan.tessera.domain.integration.payment.service.PaymentGatewayService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/integrations/payments/webhooks")
class PaymentWebhookController(
    private val paymentGatewayServices: List<PaymentGatewayService>,
) {
    private val log = LoggerFactory.getLogger(PaymentWebhookController::class.java)

    @PostMapping("/stripe")
    fun handleStripeWebhook(
        @RequestBody payload: String,
        @RequestHeader("Stripe-Signature") signature: String,
    ): ResponseEntity<String> {
        val stripeService =
            paymentGatewayServices.find { it.supports() == GatewayType.STRIPE }
                ?: return ResponseEntity.badRequest().body("Stripe is not configured")

        return try {
            stripeService.handleWebhook(payload, signature)
            ResponseEntity.ok("OK")
        } catch (e: Exception) {
            log.error("Failed to handle Stripe webhook", e)
            ResponseEntity.badRequest().body(e.message)
        }
    }

    @PostMapping("/paymongo")
    fun handlePayMongoWebhook(
        @RequestBody payload: String,
        @RequestHeader("Paymongo-Signature", required = false) signature: String?,
    ): ResponseEntity<String> {
        val payMongoService =
            paymentGatewayServices.find { it.supports() == GatewayType.PAYMONGO }
                ?: return ResponseEntity.badRequest().body("PayMongo is not configured")

        return try {
            payMongoService.handleWebhook(payload, signature ?: "")
            ResponseEntity.ok("OK")
        } catch (e: Exception) {
            log.error("Failed to handle PayMongo webhook", e)
            ResponseEntity.badRequest().body(e.message)
        }
    }
}
