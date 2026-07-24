package com.aquinofroilan.tessera.dto

import com.aquinofroilan.tessera.model.PaymentMethod
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.LocalDate

data class BillLineRequest(
    @field:NotNull(message = "Account ID is required")
    val accountId: java.util.UUID,
    @field:Positive(message = "Amount must be positive")
    val amount: BigDecimal,
    val description: String? = null,
)

data class CreateBillRequest(
    @field:NotNull(message = "Vendor ID is required")
    val vendorId: java.util.UUID,
    val date: LocalDate,
    val dueDate: LocalDate,
    val referenceNumber: String? = null,
    val taxGroupId: java.util.UUID? = null,
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
    val accountId: java.util.UUID,
    val accountCode: String,
    val accountName: String,
    val amount: BigDecimal,
    val description: String?,
)

data class BillResponse(
    val id: java.util.UUID,
    val billNumber: String,
    val vendorId: java.util.UUID,
    val vendorName: String,
    val date: String,
    val dueDate: String,
    val referenceNumber: String?,
    val taxGroupId: java.util.UUID?,
    val organizationId: java.util.UUID,
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

data class BillSummaryResponse(
    val id: java.util.UUID,
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
    val id: java.util.UUID,
    val billId: java.util.UUID,
    val paymentDate: String,
    val amount: BigDecimal,
    val baseCurrencyAmount: BigDecimal,
    val exchangeRate: BigDecimal,
    val paymentMethod: String,
    val referenceNumber: String?,
    val journalEntryId: java.util.UUID?,
    val createdBy: java.util.UUID,
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
    val vendorId: java.util.UUID,
    val vendorName: String,
    val aging: AgingBucket,
)

data class ApAgingReportResponse(
    val asOfDate: String,
    val vendors: List<VendorAgingResponse>,
    val totals: AgingBucket,
)
