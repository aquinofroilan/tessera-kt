package com.aquinofroilan.tessera.domain.integration.payment.service

import com.aquinofroilan.tessera.domain.finance.model.Invoice
import com.aquinofroilan.tessera.domain.integration.payment.dto.PaymentCheckoutResponse
import com.aquinofroilan.tessera.domain.integration.payment.model.GatewayType
import com.aquinofroilan.tessera.domain.integration.payment.model.PaymentTransaction
import com.stripe.Stripe
import com.stripe.model.Event
import com.stripe.model.checkout.Session
import com.stripe.net.Webhook
import com.stripe.param.checkout.SessionCreateParams
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class StripeGatewayServiceImpl(
    @Lazy private val orchestrator: PaymentOrchestrator,
    @Value("\${stripe.secret-key:sk_test_placeholder}") private val secretKey: String,
    @Value("\${stripe.webhook-secret:whsec_placeholder}") private val webhookSecret: String,
) : PaymentGatewayService {
    private val log = LoggerFactory.getLogger(StripeGatewayServiceImpl::class.java)

    init {
        Stripe.apiKey = secretKey
    }

    override fun supports(): GatewayType = GatewayType.STRIPE

    override fun createCheckoutSession(
        invoice: Invoice,
        successUrl: String,
        cancelUrl: String,
    ): PaymentCheckoutResponse {
        val amountInCents = invoice.totalAmount.multiply(BigDecimal(100)).toLong()
        val params =
            SessionCreateParams
                .builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .addLineItem(
                    SessionCreateParams.LineItem
                        .builder()
                        .setQuantity(1L)
                        .setPriceData(
                            SessionCreateParams.LineItem.PriceData
                                .builder()
                                .setCurrency(invoice.currencyCode.lowercase())
                                .setUnitAmount(amountInCents)
                                .setProductData(
                                    SessionCreateParams.LineItem.PriceData.ProductData
                                        .builder()
                                        .setName("Invoice ${invoice.invoiceNumber}")
                                        .build(),
                                ).build(),
                        ).build(),
                ).putMetadata("invoiceId", invoice.id.toString())
                .build()

        val session = Session.create(params)

        // Log transaction as PENDING
        val tx =
            PaymentTransaction(
                organizationId = invoice.organizationId,
                invoiceId = invoice.id,
                gateway = GatewayType.STRIPE,
                gatewayIntentId = session.id, // Session ID mapped
                grossAmount = invoice.totalAmount,
                currency = invoice.currencyCode,
            )
        orchestrator.recordPaymentTransaction(tx)

        return PaymentCheckoutResponse(
            paymentUrl = session.url,
            intentId = session.id,
        )
    }

    override fun handleWebhook(
        payload: String,
        signature: String,
    ) {
        val event: Event =
            try {
                Webhook.constructEvent(payload, signature, webhookSecret)
            } catch (e: Exception) {
                log.error("Stripe webhook signature verification failed.", e)
                throw IllegalArgumentException("Invalid signature")
            }

        if (event.type == "checkout.session.completed") {
            val session = event.dataObjectDeserializer.`object`.get() as Session
            // Note: In a real app, you would fetch the BalanceTransaction to get the exact fee.
            // For MVP, we simulate fee extraction or just use 0.0.
            val grossAmount = BigDecimal(session.amountTotal).divide(BigDecimal(100))
            val feeAmount = BigDecimal.ZERO // Placeholder for fee
            val netAmount = grossAmount.subtract(feeAmount)

            orchestrator.handleSuccessfulPayment(session.id, grossAmount, feeAmount, netAmount)
        }
    }
}
