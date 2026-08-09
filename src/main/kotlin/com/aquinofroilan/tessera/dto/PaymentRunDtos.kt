package com.aquinofroilan.tessera.dto

import com.aquinofroilan.tessera.model.PaymentRun
import com.aquinofroilan.tessera.model.PaymentRunLine
import com.aquinofroilan.tessera.model.PaymentRunLineStatus
import com.aquinofroilan.tessera.model.PaymentRunStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDate

data class CreatePaymentRunRequest(
    @field:NotBlank(message = "Code is required")
    val code: String,
    @field:NotBlank(message = "Bank account ID is required")
    val bankAccountId: java.util.UUID,
    @field:NotNull(message = "Run date is required")
    val runDate: LocalDate?,
    @field:NotEmpty(message = "At least one bill ID is required")
    val billIds: List<java.util.UUID>,
    val notes: String? = null,
)

data class PaymentRunLineResponse(
    val id: java.util.UUID,
    val lineNumber: Int,
    val billId: java.util.UUID,
    val vendorId: java.util.UUID,
    val vendorName: String,
    val billNumber: String,
    val amount: BigDecimal,
    val status: PaymentRunLineStatus,
    val billPaymentId: java.util.UUID?,
    val notes: String?,
) {
    companion object {
        fun from(l: PaymentRunLine) =
            PaymentRunLineResponse(
                id = l.id,
                lineNumber = l.lineNumber,
                billId = l.billId,
                vendorId = l.vendorId,
                vendorName = l.vendorName,
                billNumber = l.billNumber,
                amount = l.amount,
                status = l.status,
                billPaymentId = l.billPaymentId,
                notes = l.notes,
            )
    }
}

data class PaymentRunResponse(
    val id: java.util.UUID,
    val code: String,
    val bankAccountId: java.util.UUID,
    val runDate: LocalDate,
    val status: PaymentRunStatus,
    val totalAmount: BigDecimal,
    val currency: String,
    val notes: String?,
    val lines: List<PaymentRunLineResponse>,
) {
    companion object {
        fun from(p: PaymentRun) =
            PaymentRunResponse(
                id = p.id,
                code = p.code,
                bankAccountId = p.bankAccountId,
                runDate = p.runDate,
                status = p.status,
                totalAmount = p.totalAmount,
                currency = p.currency,
                notes = p.notes,
                lines = p.lines.map { PaymentRunLineResponse.from(it) },
            )
    }
}
