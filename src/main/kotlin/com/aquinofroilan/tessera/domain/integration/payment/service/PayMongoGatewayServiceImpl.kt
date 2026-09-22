package com.aquinofroilan.tessera.domain.integration.payment.service

import com.aquinofroilan.tessera.domain.finance.model.Invoice
import com.aquinofroilan.tessera.domain.integration.payment.dto.PaymentCheckoutResponse
import com.aquinofroilan.tessera.domain.integration.payment.model.GatewayType
import com.aquinofroilan.tessera.domain.integration.payment.model.PaymentTransaction
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.math.BigDecimal

/**
 * PayMongo integration using a mock RestClient for MVP purposes.
 * In a fully working version, this uses Spring RestClient to hit https://api.paymongo.com/v1/links.
 */
@Service
class PayMongoGatewayServiceImpl(
    private val orchestrator: PaymentOrchestrator,
    @Value("\${paymongo.secret-key:sk_test_placeholder}") private val secretKey: String,
    @Value("\${paymongo.webhook-secret:whsec_placeholder}") private val webhookSecret: String,
) : PaymentGatewayService {
    private val log = LoggerFactory.getLogger(PayMongoGatewayServiceImpl::class.java)

    override fun supports(): GatewayType = GatewayType.PAYMONGO

    override fun createCheckoutSession(
        invoice: Invoice,
        successUrl: String,
        cancelUrl: String,
    ): PaymentCheckoutResponse {
        val amountInCents = invoice.totalAmount.multiply(BigDecimal(100)).toLong()
        // Simulated API Call to PayMongo
        val mockLinkId =
            "link_" +
                java.util.UUID
                    .randomUUID()
                    .toString()
                    .replace("-", "")
                    .take(16)
        val mockUrl = "https://pm.link/mock/$mockLinkId"

        val tx =
            PaymentTransaction(
                organizationId = invoice.organizationId,
                invoiceId = invoice.id,
                gateway = GatewayType.PAYMONGO,
                gatewayIntentId = mockLinkId,
                grossAmount = invoice.totalAmount,
                currency = invoice.currencyCode,
            )
        orchestrator.recordPaymentTransaction(tx)

        return PaymentCheckoutResponse(
            paymentUrl = mockUrl,
            intentId = mockLinkId,
        )
    }

    override fun handleWebhook(
        payload: String,
        signature: String,
    ) {
        log.info("PayMongo webhook received. Signature validation would happen here.")

        // Simulating the extraction of intent ID and amounts
        // In real life, parse payload with Jackson to get attributes.type == "link.payment.paid"
        // and extract data.attributes.amount.
    }
}
