package com.aquinofroilan.tessera.domain.integration.payment.service

import com.aquinofroilan.tessera.domain.finance.model.Invoice
import com.aquinofroilan.tessera.domain.integration.payment.dto.PaymentCheckoutResponse
import com.aquinofroilan.tessera.domain.integration.payment.model.GatewayType

interface PaymentGatewayService {
    /**
     * Identifies the gateway this service supports.
     */
    fun supports(): GatewayType

    /**
     * Creates a checkout session/payment intent and returns the URL for the user to visit.
     */
    fun createCheckoutSession(
        invoice: Invoice,
        successUrl: String,
        cancelUrl: String,
    ): PaymentCheckoutResponse

    /**
     * Parses the raw webhook payload, verifies its signature, and delegates to the orchestrator to update AR.
     */
    fun handleWebhook(
        payload: String,
        signature: String,
    )
}
