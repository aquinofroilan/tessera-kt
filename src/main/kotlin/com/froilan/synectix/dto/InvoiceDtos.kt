package com.froilan.synectix.dto

import com.froilan.synectix.model.PaymentMethod
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.LocalDate

data class InvoiceLineRequest(
    @field:NotBlank(message = "Account ID is required")
    val accountId: String,
    @field:Positive(message = "Amount must be positive")
    val amount: BigDecimal,
    val description: String? = null,
)

data class CreateInvoiceRequest(
    @field:NotBlank(message = "Customer ID is required")
    val customerId: String,
    val date: LocalDate,
    val dueDate: LocalDate,
    val referenceNumber: String? = null,
    val taxGroupId: String? = null,
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
    val accountId: String,
    val accountCode: String,
    val accountName: String,
    val amount: BigDecimal,
    val description: String?,
)

data class InvoiceResponse(
    val id: String,
    val invoiceNumber: String,
    val customerId: String,
    val customerName: String,
    val date: String,
    val dueDate: String,
    val referenceNumber: String?,
    val taxGroupId: String?,
    val organizationId: String,
    val status: String,
    val lines: List<InvoiceLineResponse>,
    val totalAmount: BigDecimal,
    val taxAmount: BigDecimal,
    val amountReceived: BigDecimal,
    val journalEntryId: String?,
    val createdBy: String,
    val approvedAt: String?,
    val approvedBy: String?,
    val paidAt: String?,
    val voidedAt: String?,
    val voidedBy: String?,
    val voidReason: String?,
    val createdAt: String?,
    val updatedAt: String?,
)

data class InvoiceSummaryResponse(
    val id: String,
    val invoiceNumber: String,
    val customerName: String,
    val date: String,
    val dueDate: String,
    val status: String,
    val totalAmount: BigDecimal,
    val taxAmount: BigDecimal,
    val amountReceived: BigDecimal,
)

data class InvoiceReceiptResponse(
    val id: String,
    val invoiceId: String,
    val receiptDate: String,
    val amount: BigDecimal,
    val paymentMethod: String,
    val referenceNumber: String?,
    val journalEntryId: String?,
    val createdBy: String,
    val createdAt: String?,
)

data class CustomerAgingResponse(
    val customerId: String,
    val customerName: String,
    val aging: AgingBucket,
)

data class ArAgingReportResponse(
    val asOfDate: String,
    val customers: List<CustomerAgingResponse>,
    val totals: AgingBucket,
)
