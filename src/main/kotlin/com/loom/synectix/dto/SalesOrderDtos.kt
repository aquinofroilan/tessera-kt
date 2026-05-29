package com.loom.synectix.dto

import com.loom.synectix.model.SalesOrder
import com.loom.synectix.model.SalesOrderStatus
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.LocalDate

data class CreateSalesOrderLineRequest(
    @field:NotBlank(message = "Product ID is required")
    val productId: String,
    @field:NotNull(message = "Quantity is required")
    @field:Positive(message = "Quantity must be positive")
    val quantity: BigDecimal?,
    @field:NotNull(message = "Unit price is required")
    val unitPrice: BigDecimal?,
    val description: String? = null,
)

data class CreateSalesOrderRequest(
    @field:NotBlank(message = "Customer ID is required")
    val customerId: String,
    @field:NotBlank(message = "Warehouse ID is required")
    val warehouseId: String,
    @field:NotNull(message = "Order date is required")
    val orderDate: LocalDate?,
    val expectedDate: LocalDate? = null,
    val referenceNumber: String? = null,
    @field:NotEmpty(message = "At least one line item is required")
    @field:Valid
    val lines: List<CreateSalesOrderLineRequest>,
)

data class SalesOrderLineResponse(
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

data class SalesOrderResponse(
    val id: String,
    val soNumber: String,
    val customerId: String,
    val customerName: String,
    val warehouseId: String,
    val orderDate: String,
    val expectedDate: String?,
    val referenceNumber: String?,
    val organizationId: String,
    val status: SalesOrderStatus,
    val lines: List<SalesOrderLineResponse>,
    val totalAmount: BigDecimal,
    val createdBy: String,
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
