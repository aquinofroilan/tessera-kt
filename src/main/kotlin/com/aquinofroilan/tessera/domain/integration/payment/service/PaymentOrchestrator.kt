package com.aquinofroilan.tessera.domain.integration.payment.service

import com.aquinofroilan.tessera.domain.finance.model.InvoiceReceipt
import com.aquinofroilan.tessera.domain.finance.model.InvoiceStatus
import com.aquinofroilan.tessera.domain.finance.model.PaymentMethod
import com.aquinofroilan.tessera.domain.finance.repository.InvoiceReceiptRepository
import com.aquinofroilan.tessera.domain.finance.repository.InvoiceRepository
import com.aquinofroilan.tessera.domain.integration.payment.dto.PaymentCheckoutRequest
import com.aquinofroilan.tessera.domain.integration.payment.dto.PaymentCheckoutResponse
import com.aquinofroilan.tessera.domain.integration.payment.model.PaymentTransaction
import com.aquinofroilan.tessera.domain.integration.payment.model.PaymentTransactionStatus
import com.aquinofroilan.tessera.domain.integration.payment.repository.PaymentTransactionRepository
import com.aquinofroilan.tessera.security.AuthenticationContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

@Service
class PaymentOrchestrator(
    private val paymentGatewayServices: List<PaymentGatewayService>,
    private val paymentTransactionRepository: PaymentTransactionRepository,
    private val invoiceRepository: InvoiceRepository,
    private val invoiceReceiptRepository: InvoiceReceiptRepository,
    private val authContext: AuthenticationContext,
) {
    @Transactional
    fun createCheckoutSession(
        request: PaymentCheckoutRequest,
        successUrl: String,
        cancelUrl: String,
    ): PaymentCheckoutResponse {
        val invoice =
            invoiceRepository.findById(request.invoiceId!!).orElseThrow {
                IllegalArgumentException("Invoice not found")
            }

        if (invoice.status == InvoiceStatus.PAID) {
            throw IllegalStateException("Invoice is already paid")
        }

        val gatewayService =
            paymentGatewayServices.find { it.supports() == request.gateway }
                ?: throw IllegalArgumentException("Gateway not supported: ${request.gateway}")

        return gatewayService.createCheckoutSession(invoice, successUrl, cancelUrl)
    }

    @Transactional
    fun recordPaymentTransaction(transaction: PaymentTransaction) {
        paymentTransactionRepository.save(transaction)
    }

    @Transactional
    fun handleSuccessfulPayment(
        gatewayIntentId: String,
        grossAmount: BigDecimal,
        feeAmount: BigDecimal,
        netAmount: BigDecimal,
    ) {
        val transaction = paymentTransactionRepository.findByGatewayIntentId(gatewayIntentId) ?: return

        if (transaction.status == PaymentTransactionStatus.SUCCEEDED) {
            return // Idempotent check
        }

        transaction.status = PaymentTransactionStatus.SUCCEEDED
        transaction.grossAmount = grossAmount
        transaction.feeAmount = feeAmount
        transaction.netAmount = netAmount
        paymentTransactionRepository.save(transaction)

        val invoice =
            invoiceRepository.findById(transaction.invoiceId).orElseThrow {
                IllegalStateException("Invoice not found for transaction")
            }

        // Create InvoiceReceipt
        val receipt =
            InvoiceReceipt(
                invoiceId = invoice.id,
                receiptDate = LocalDate.now(),
                amount = transaction.grossAmount,
                baseCurrencyAmount = transaction.grossAmount,
                exchangeRate = BigDecimal.ONE,
                paymentMethod = PaymentMethod.CREDIT_CARD, // Or OTHER
                referenceNumber = transaction.gatewayIntentId,
                organizationId = transaction.organizationId,
                createdBy = transaction.organizationId, // Fallback to org ID since this is a system webhook
            )
        invoiceReceiptRepository.save(receipt)

        // Update Invoice status
        val totalReceipts =
            invoiceReceiptRepository
                .findByInvoiceIdAndOrganizationId(invoice.id, invoice.organizationId)
                .sumOf { it.amount }

        if (totalReceipts >= invoice.totalAmount) {
            invoice.status = InvoiceStatus.PAID
        } else {
            invoice.status = InvoiceStatus.PARTIALLY_PAID
        }
        invoiceRepository.save(invoice)
    }
}
