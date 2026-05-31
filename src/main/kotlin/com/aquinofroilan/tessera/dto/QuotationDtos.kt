package com.aquinofroilan.tessera.dto

import com.aquinofroilan.tessera.model.Quotation
import com.aquinofroilan.tessera.model.QuotationStatus
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.LocalDate

data class CreateQuotationLineRequest(
    @field:NotBlank(message = "Product ID is required")
    val productId: String,
    @field:NotNull(message = "Quantity is required")
    @field:Positive(message = "Quantity must be positive")
    val quantity: BigDecimal?,
    @field:NotNull(message = "Unit price is required")
    val unitPrice: BigDecimal?,
    val description: String? = null,
)

data class CreateQuotationRequest(
    @field:NotBlank(message = "Customer ID is required")
    val customerId: String,
    val warehouseId: String? = null,
    @field:NotNull(message = "Quote date is required")
    val quoteDate: LocalDate?,
    val validUntil: LocalDate? = null,
    val referenceNumber: String? = null,
    @field:NotEmpty(message = "At least one line item is required")
    @field:Valid
    val lines: List<CreateQuotationLineRequest>,
)

data class RejectQuotationRequest(
    val reason: String? = null,
)

/**
 * Options applied when converting an accepted quote into a sales order. The
 * warehouse and order date may be supplied here or fall back to the quote's
 * warehouse / today.
 */
data class ConvertQuotationRequest(
    val warehouseId: String? = null,
    val orderDate: LocalDate? = null,
    val expectedDate: LocalDate? = null,
)

data class QuotationLineResponse(
    val id: String,
    val lineNumber: Int,
    val productId: String,
    val productSku: String,
    val productName: String,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val lineTotal: BigDecimal,
    val description: String?,
)

data class QuotationResponse(
    val id: String,
    val quoteNumber: String,
    val customerId: String,
    val customerName: String,
    val warehouseId: String?,
    val quoteDate: String,
    val validUntil: String?,
    val referenceNumber: String?,
    val organizationId: String,
    val status: QuotationStatus,
    val lines: List<QuotationLineResponse>,
    val totalAmount: BigDecimal,
    val createdBy: String,
    val sentAt: String?,
    val decidedAt: String?,
    val decisionReason: String?,
    val convertedSalesOrderId: String?,
    val createdAt: String?,
    val updatedAt: String?,
) {
    companion object {
        fun from(quote: Quotation) =
            QuotationResponse(
                id = quote.id,
                quoteNumber = quote.quoteNumber,
                customerId = quote.customerId,
                customerName = quote.customerName,
                warehouseId = quote.warehouseId,
                quoteDate = quote.quoteDate.toString(),
                validUntil = quote.validUntil?.toString(),
                referenceNumber = quote.referenceNumber,
                organizationId = quote.organizationId,
                status = quote.status,
                lines =
                    quote.lines.map { line ->
                        QuotationLineResponse(
                            id = line.id,
                            lineNumber = line.lineNumber,
                            productId = line.productId,
                            productSku = line.productSku,
                            productName = line.productName,
                            quantity = line.quantity,
                            unitPrice = line.unitPrice,
                            lineTotal = line.lineTotal,
                            description = line.description,
                        )
                    },
                totalAmount = quote.totalAmount,
                createdBy = quote.createdBy,
                sentAt = quote.sentAt?.toString(),
                decidedAt = quote.decidedAt?.toString(),
                decisionReason = quote.decisionReason,
                convertedSalesOrderId = quote.convertedSalesOrderId,
                createdAt = quote.createdAt?.toString(),
                updatedAt = quote.updatedAt?.toString(),
            )
    }
}
