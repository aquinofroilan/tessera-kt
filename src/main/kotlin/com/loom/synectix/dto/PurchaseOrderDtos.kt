package com.loom.synectix.dto

import com.loom.synectix.model.PurchaseOrder
import com.loom.synectix.model.PurchaseOrderStatus
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.LocalDate

data class CreatePurchaseOrderLineRequest(
    @field:NotBlank(message = "Product ID is required")
    val productId: String,
    @field:NotNull(message = "Quantity is required")
    @field:Positive(message = "Quantity must be positive")
    val quantity: BigDecimal?,
    @field:NotNull(message = "Unit cost is required")
    val unitCost: BigDecimal?,
    val description: String? = null,
)

data class CreatePurchaseOrderRequest(
    @field:NotBlank(message = "Vendor ID is required")
    val vendorId: String,
    @field:NotBlank(message = "Warehouse ID is required")
    val warehouseId: String,
    @field:NotNull(message = "Order date is required")
    val orderDate: LocalDate?,
    val expectedDate: LocalDate? = null,
    val referenceNumber: String? = null,
    @field:NotEmpty(message = "At least one line item is required")
    @field:Valid
    val lines: List<CreatePurchaseOrderLineRequest>,
)

data class PurchaseOrderLineResponse(
    val id: String,
    val lineNumber: Int,
    val productId: String,
    val productSku: String,
    val productName: String,
    val quantity: BigDecimal,
    val unitCost: BigDecimal,
    val lineTotal: BigDecimal,
    val description: String?,
)

data class PurchaseOrderResponse(
    val id: String,
    val poNumber: String,
    val vendorId: String,
    val vendorName: String,
    val warehouseId: String,
    val orderDate: String,
    val expectedDate: String?,
    val referenceNumber: String?,
    val organizationId: String,
    val status: PurchaseOrderStatus,
    val lines: List<PurchaseOrderLineResponse>,
    val totalAmount: BigDecimal,
    val createdBy: String,
    val approvedAt: String?,
    val receivedAt: String?,
    val cancelledAt: String?,
    val createdAt: String?,
    val updatedAt: String?,
) {
    companion object {
        fun from(po: PurchaseOrder) =
            PurchaseOrderResponse(
                id = po.id,
                poNumber = po.poNumber,
                vendorId = po.vendorId,
                vendorName = po.vendorName,
                warehouseId = po.warehouseId,
                orderDate = po.orderDate.toString(),
                expectedDate = po.expectedDate?.toString(),
                referenceNumber = po.referenceNumber,
                organizationId = po.organizationId,
                status = po.status,
                lines =
                    po.lines.map { line ->
                        PurchaseOrderLineResponse(
                            id = line.id,
                            lineNumber = line.lineNumber,
                            productId = line.productId,
                            productSku = line.productSku,
                            productName = line.productName,
                            quantity = line.quantity,
                            unitCost = line.unitCost,
                            lineTotal = line.lineTotal,
                            description = line.description,
                        )
                    },
                totalAmount = po.totalAmount,
                createdBy = po.createdBy,
                approvedAt = po.approvedAt?.toString(),
                receivedAt = po.receivedAt?.toString(),
                cancelledAt = po.cancelledAt?.toString(),
                createdAt = po.createdAt?.toString(),
                updatedAt = po.updatedAt?.toString(),
            )
    }
}
