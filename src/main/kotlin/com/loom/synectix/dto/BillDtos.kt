package com.loom.synectix.dto

import com.loom.synectix.model.PaymentMethod
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.LocalDate

data class BillLineRequest(
    @field:NotBlank(message = "Account ID is required")
    val accountId: String,
    @field:Positive(message = "Amount must be positive")
    val amount: BigDecimal,
    val description: String? = null,
)

data class CreateBillRequest(
    @field:NotBlank(message = "Vendor ID is required")
    val vendorId: String,
    val date: LocalDate,
    val dueDate: LocalDate,
    val referenceNumber: String? = null,
    val taxGroupId: String? = null,
    val currencyCode: String? = null,
    @field:NotEmpty(message = "At least one line item is required")
    @field:Valid
    val lines: List<BillLineRequest>,
)

data class VoidBillRequest(
    @field:NotBlank(message = "Void reason is required")
    val reason: String,
)

data class RecordPaymentRequest(
    val paymentDate: LocalDate,
    @field:Positive(message = "Payment amount must be positive")
    val amount: BigDecimal,
    val paymentMethod: PaymentMethod,
    val referenceNumber: String? = null,
)

data class BillLineResponse(
    val accountId: String,
    val accountCode: String,
    val accountName: String,
    val amount: BigDecimal,
    val description: String?,
)

data class BillResponse(
    val id: String,
    val billNumber: String,
    val vendorId: String,
    val vendorName: String,
    val date: String,
    val dueDate: String,
    val referenceNumber: String?,
    val taxGroupId: String?,
    val organizationId: String,
    val status: String,
    val lines: List<BillLineResponse>,
    val totalAmount: BigDecimal,
    val taxAmount: BigDecimal,
    val amountPaid: BigDecimal,
    val currencyCode: String,
    val exchangeRate: BigDecimal,
    val baseCurrencyAmount: BigDecimal,
    val baseCurrencyTaxAmount: BigDecimal,
    val baseCurrencyAmountPaid: BigDecimal,
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

data class BillSummaryResponse(
    val id: String,
    val billNumber: String,
    val vendorName: String,
    val date: String,
    val dueDate: String,
    val status: String,
    val totalAmount: BigDecimal,
    val taxAmount: BigDecimal,
    val amountPaid: BigDecimal,
    val currencyCode: String,
)

data class BillPaymentResponse(
    val id: String,
    val billId: String,
    val paymentDate: String,
    val amount: BigDecimal,
    val baseCurrencyAmount: BigDecimal,
    val exchangeRate: BigDecimal,
    val paymentMethod: String,
    val referenceNumber: String?,
    val journalEntryId: String?,
    val createdBy: String,
    val createdAt: String?,
)

data class AgingBucket(
    val current: BigDecimal,
    val days1to30: BigDecimal,
    val days31to60: BigDecimal,
    val days61to90: BigDecimal,
    val days90plus: BigDecimal,
    val total: BigDecimal,
)

data class VendorAgingResponse(
    val vendorId: String,
    val vendorName: String,
    val aging: AgingBucket,
)

data class ApAgingReportResponse(
    val asOfDate: String,
    val vendors: List<VendorAgingResponse>,
    val totals: AgingBucket,
)
