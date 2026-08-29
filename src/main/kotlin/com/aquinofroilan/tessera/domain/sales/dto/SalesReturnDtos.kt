package com.aquinofroilan.tessera.domain.sales.dto

import com.aquinofroilan.tessera.domain.sales.model.ReturnReason
import com.aquinofroilan.tessera.domain.sales.model.SalesReturn
import com.aquinofroilan.tessera.domain.sales.model.SalesReturnLine
import com.aquinofroilan.tessera.domain.sales.model.SalesReturnStatus
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class SalesReturnLineDto(
    val id: UUID,
    val lineNumber: Int,
    val productId: UUID,
    val productSku: String,
    val productName: String,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val lineTotal: BigDecimal,
    val receivedQuantity: BigDecimal,
    val conditionNotes: String?,
) {
    companion object {
        fun from(line: SalesReturnLine): SalesReturnLineDto =
            SalesReturnLineDto(
                id = line.id,
                lineNumber = line.lineNumber,
                productId = line.productId,
                productSku = line.productSku,
                productName = line.productName,
                quantity = line.quantity,
                unitPrice = line.unitPrice,
                lineTotal = line.lineTotal,
                receivedQuantity = line.receivedQuantity,
                conditionNotes = line.conditionNotes,
            )
    }
}

data class SalesReturnResponse(
    val id: UUID,
    val organizationId: UUID,
    val returnNumber: String,
    val customerId: UUID,
    val customerName: String,
    val salesOrderId: UUID?,
    val invoiceId: UUID?,
    val warehouseId: UUID,
    val returnDate: LocalDate,
    val status: SalesReturnStatus,
    val reason: ReturnReason,
    val notes: String?,
    val restockInventory: Boolean,
    val totalAmount: BigDecimal,
    val createdBy: UUID,
    val approvedBy: UUID?,
    val approvedAt: LocalDateTime?,
    val receivedBy: UUID?,
    val receivedAt: LocalDateTime?,
    val lines: List<SalesReturnLineDto>,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
) {
    companion object {
        fun from(salesReturn: SalesReturn): SalesReturnResponse =
            SalesReturnResponse(
                id = salesReturn.id,
                organizationId = salesReturn.organizationId,
                returnNumber = salesReturn.returnNumber,
                customerId = salesReturn.customerId,
                customerName = salesReturn.customerName,
                salesOrderId = salesReturn.salesOrderId,
                invoiceId = salesReturn.invoiceId,
                warehouseId = salesReturn.warehouseId,
                returnDate = salesReturn.returnDate,
                status = salesReturn.status,
                reason = salesReturn.reason,
                notes = salesReturn.notes,
                restockInventory = salesReturn.restockInventory,
                totalAmount = salesReturn.totalAmount,
                createdBy = salesReturn.createdBy,
                approvedBy = salesReturn.approvedBy,
                approvedAt = salesReturn.approvedAt,
                receivedBy = salesReturn.receivedBy,
                receivedAt = salesReturn.receivedAt,
                lines = salesReturn.lines.map { SalesReturnLineDto.from(it) },
                createdAt = salesReturn.createdAt,
                updatedAt = salesReturn.updatedAt,
            )
    }
}

data class CreateSalesReturnLineRequest(
    @field:NotNull(message = "Product ID is required")
    val productId: UUID,
    @field:NotNull(message = "Quantity is required")
    @field:DecimalMin(value = "0.0001", message = "Quantity must be greater than zero")
    val quantity: BigDecimal,
    @field:NotNull(message = "Unit price is required")
    @field:DecimalMin(value = "0.0", message = "Unit price cannot be negative")
    val unitPrice: BigDecimal,
    val conditionNotes: String? = null,
)

data class CreateSalesReturnRequest(
    @field:NotNull(message = "Customer ID is required")
    val customerId: UUID,
    val salesOrderId: UUID? = null,
    val invoiceId: UUID? = null,
    @field:NotNull(message = "Warehouse ID is required")
    val warehouseId: UUID,
    val returnDate: LocalDate? = null,
    @field:NotNull(message = "Return reason is required")
    val reason: ReturnReason,
    val notes: String? = null,
    val restockInventory: Boolean? = true,
    @field:NotEmpty(message = "Return lines cannot be empty")
    val lines: List<CreateSalesReturnLineRequest>,
)

data class ReceiveSalesReturnRequest(
    val conditionNotes: String? = null,
)
