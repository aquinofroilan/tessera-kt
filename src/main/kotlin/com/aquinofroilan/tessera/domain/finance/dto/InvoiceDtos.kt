package com.aquinofroilan.tessera.domain.finance.dto

import com.aquinofroilan.tessera.domain.finance.model.PaymentMethod
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.LocalDate

data class InvoiceLineRequest(
    @field:NotNull(message = "Account ID is required")
    val accountId: java.util.UUID,
    @field:Positive(message = "Amount must be positive")
    val amount: BigDecimal,
    val description: String? = null,
)

data class CreateInvoiceRequest(
    @field:NotNull(message = "Customer ID is required")
    val customerId: java.util.UUID,
    val date: LocalDate,
    val dueDate: LocalDate,
    val referenceNumber: String? = null,
    val taxGroupId: java.util.UUID? = null,
    val currencyCode: String? = null,
    @field:NotEmpty(message = "At least one line item is required")
    @field:Valid
    val lines: List<InvoiceLineRequest>,
)

data class VoidInvoiceRequest(
    @field:NotBlank(message = "Void reason is required")
    val reason: String,
)

data class RecordReceiptRequest(
    val receiptDate: LocalDate,
    @field:Positive(message = "Receipt amount must be positive")
    val amount: BigDecimal,
    val paymentMethod: PaymentMethod,
    val referenceNumber: String? = null,
)

data class InvoiceLineResponse(
    val accountId: java.util.UUID,
    val accountCode: String,
    val accountName: String,
    val amount: BigDecimal,
    val description: String?,
)

data class InvoiceResponse(
    val id: java.util.UUID,
    val invoiceNumber: String,
    val customerId: java.util.UUID,
    val customerName: String,
    val date: String,
    val dueDate: String,
    val referenceNumber: String?,
    val taxGroupId: java.util.UUID?,
    val organizationId: java.util.UUID,
    val status: String,
    val lines: List<InvoiceLineResponse>,
    val totalAmount: BigDecimal,
    val taxAmount: BigDecimal,
    val amountReceived: BigDecimal,
    val currencyCode: String,
    val exchangeRate: BigDecimal,
    val baseCurrencyAmount: BigDecimal,
    val baseCurrencyTaxAmount: BigDecimal,
    val baseCurrencyAmountReceived: BigDecimal,
    val journalEntryId: java.util.UUID?,
    val createdBy: java.util.UUID,
    val approvedAt: String?,
    val approvedBy: java.util.UUID?,
    val paidAt: String?,
    val voidedAt: String?,
    val voidedBy: java.util.UUID?,
    val voidReason: String?,
    val createdAt: String?,
    val updatedAt: String?,
)

data class InvoiceSummaryResponse(
    val id: java.util.UUID,
    val invoiceNumber: String,
    val customerName: String,
    val date: String,
    val dueDate: String,
    val status: String,
    val totalAmount: BigDecimal,
    val taxAmount: BigDecimal,
    val amountReceived: BigDecimal,
    val currencyCode: String,
)

data class InvoiceReceiptResponse(
    val id: java.util.UUID,
    val invoiceId: java.util.UUID,
    val receiptDate: String,
    val amount: BigDecimal,
    val baseCurrencyAmount: BigDecimal,
    val exchangeRate: BigDecimal,
    val paymentMethod: String,
    val referenceNumber: String?,
    val journalEntryId: java.util.UUID?,
    val createdBy: java.util.UUID,
    val createdAt: String?,
)

data class CustomerAgingResponse(
    val customerId: java.util.UUID,
    val customerName: String,
    val aging: AgingBucket,
)

data class ArAgingReportResponse(
    val asOfDate: String,
    val customers: List<CustomerAgingResponse>,
    val totals: AgingBucket,
)
