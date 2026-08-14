package com.aquinofroilan.tessera.domain.sales.dto

import com.aquinofroilan.tessera.domain.sales.model.SalesOrder
import com.aquinofroilan.tessera.domain.sales.model.SalesOrderStatus
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.LocalDate

data class CreateSalesOrderLineRequest(
    @field:NotNull(message = "Product ID is required")
    val productId: java.util.UUID,
    @field:NotNull(message = "Quantity is required")
    @field:Positive(message = "Quantity must be positive")
    val quantity: BigDecimal?,
    @field:NotNull(message = "Unit price is required")
    val unitPrice: BigDecimal?,
    val description: String? = null,
)

data class CreateSalesOrderRequest(
    @field:NotNull(message = "Customer ID is required")
    val customerId: java.util.UUID,
    @field:NotNull(message = "Warehouse ID is required")
    val warehouseId: java.util.UUID,
    @field:NotNull(message = "Order date is required")
    val orderDate: LocalDate?,
    val expectedDate: LocalDate? = null,
    val referenceNumber: String? = null,
    @field:NotEmpty(message = "At least one line item is required")
    @field:Valid
    val lines: List<CreateSalesOrderLineRequest>,
)

data class FulfillSalesOrderLine(
    @field:NotNull(message = "Line ID is required")
    val lineId: java.util.UUID,
    @field:NotNull(message = "Quantity is required")
    @field:Positive(message = "Quantity must be positive")
    val quantity: BigDecimal?,
)

data class FulfillSalesOrderRequest(
    @field:Valid
    val lines: List<FulfillSalesOrderLine>? = null,
)

data class GenerateInvoiceLine(
    @field:NotNull(message = "Line ID is required")
    val lineId: java.util.UUID,
    @field:NotNull(message = "Quantity is required")
    @field:Positive(message = "Quantity must be positive")
    val quantity: BigDecimal?,
    val unitPrice: BigDecimal? = null,
)

data class GenerateInvoiceRequest(
    @field:Valid
    val lines: List<GenerateInvoiceLine>? = null,
)

data class SalesOrderLineResponse(
    val id: java.util.UUID,
    val lineNumber: Int,
    val productId: java.util.UUID,
    val productSku: String,
    val productName: String,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val lineTotal: BigDecimal,
    val fulfilledQuantity: BigDecimal,
    val invoicedQuantity: BigDecimal,
    val description: String?,
)

data class SalesOrderResponse(
    val id: java.util.UUID,
    val soNumber: String,
    val customerId: java.util.UUID,
    val customerName: String,
    val warehouseId: java.util.UUID,
    val orderDate: String,
    val expectedDate: String?,
    val referenceNumber: String?,
    val organizationId: java.util.UUID,
    val status: SalesOrderStatus,
    val lines: List<SalesOrderLineResponse>,
    val totalAmount: BigDecimal,
    val createdBy: java.util.UUID,
    val approvedAt: String?,
    val fulfilledAt: String?,
    val cancelledAt: String?,
    val createdAt: String?,
    val updatedAt: String?,
) {
    companion object {
        fun from(so: SalesOrder) =
            SalesOrderResponse(
                id = so.id,
                soNumber = so.soNumber,
                customerId = so.customerId,
                customerName = so.customerName,
                warehouseId = so.warehouseId,
                orderDate = so.orderDate.toString(),
                expectedDate = so.expectedDate?.toString(),
                referenceNumber = so.referenceNumber,
                organizationId = so.organizationId,
                status = so.status,
                lines =
                    so.lines.map { line ->
                        SalesOrderLineResponse(
                            id = line.id,
                            lineNumber = line.lineNumber,
                            productId = line.productId,
                            productSku = line.productSku,
                            productName = line.productName,
                            quantity = line.quantity,
                            unitPrice = line.unitPrice,
                            lineTotal = line.lineTotal,
                            fulfilledQuantity = line.fulfilledQuantity,
                            invoicedQuantity = line.invoicedQuantity,
                            description = line.description,
                        )
                    },
                totalAmount = so.totalAmount,
                createdBy = so.createdBy,
                approvedAt = so.approvedAt?.toString(),
                fulfilledAt = so.fulfilledAt?.toString(),
                cancelledAt = so.cancelledAt?.toString(),
                createdAt = so.createdAt?.toString(),
                updatedAt = so.updatedAt?.toString(),
            )
    }
}
