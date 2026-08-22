package com.aquinofroilan.tessera.domain.procurement.dto

import com.aquinofroilan.tessera.domain.procurement.model.PurchaseOrder
import com.aquinofroilan.tessera.domain.procurement.model.PurchaseOrderStatus
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.LocalDate

data class CreatePurchaseOrderLineRequest(
    @field:NotNull(message = "Product ID is required")
    val productId: java.util.UUID,
    @field:NotNull(message = "Quantity is required")
    @field:Positive(message = "Quantity must be positive")
    val quantity: BigDecimal?,
    @field:NotNull(message = "Unit cost is required")
    val unitCost: BigDecimal?,
    val description: String? = null,
)

data class CreatePurchaseOrderRequest(
    @field:NotNull(message = "Vendor ID is required")
    val vendorId: java.util.UUID,
    @field:NotNull(message = "Warehouse ID is required")
    val warehouseId: java.util.UUID,
    @field:NotNull(message = "Order date is required")
    val orderDate: LocalDate?,
    val expectedDate: LocalDate? = null,
    val referenceNumber: String? = null,
    @field:NotEmpty(message = "At least one line item is required")
    @field:Valid
    val lines: List<CreatePurchaseOrderLineRequest>,
)

data class ReceivePurchaseOrderLine(
    @field:NotNull(message = "Line ID is required")
    val lineId: java.util.UUID,
    @field:NotNull(message = "Quantity is required")
    @field:Positive(message = "Quantity must be positive")
    val quantity: BigDecimal?,
)

data class ReceivePurchaseOrderRequest(
    @field:Valid
    val lines: List<ReceivePurchaseOrderLine>? = null,
)

data class GenerateBillLine(
    @field:NotNull(message = "Line ID is required")
    val lineId: java.util.UUID,
    @field:NotNull(message = "Quantity is required")
    @field:Positive(message = "Quantity must be positive")
    val quantity: BigDecimal?,
    val unitCost: BigDecimal? = null,
)

data class GenerateBillRequest(
    @field:Valid
    val lines: List<GenerateBillLine>? = null,
)

enum class MatchStatus {
    MATCHED,
    PRICE_VARIANCE,
    OVER_BILLED,
}

data class BillMatchLineRequest(
    @field:NotNull(message = "Line ID is required")
    val lineId: java.util.UUID,
    @field:NotNull(message = "Quantity is required")
    @field:Positive(message = "Quantity must be positive")
    val quantity: BigDecimal?,
    @field:NotNull(message = "Unit cost is required")
    val unitCost: BigDecimal?,
)

data class BillMatchRequest(
    @field:NotEmpty(message = "At least one line is required")
    @field:Valid
    val lines: List<BillMatchLineRequest>,
)

data class BillMatchLineResult(
    val lineId: java.util.UUID,
    val productSku: String,
    val orderedQuantity: BigDecimal,
    val receivedQuantity: BigDecimal,
    val billedQuantity: BigDecimal,
    val billableQuantity: BigDecimal,
    val poUnitCost: BigDecimal,
    val vendorUnitCost: BigDecimal,
    val vendorQuantity: BigDecimal,
    val status: MatchStatus,
)

data class BillMatchResult(
    val purchaseOrderId: java.util.UUID,
    val poNumber: String,
    val matched: Boolean,
    val lines: List<BillMatchLineResult>,
)

data class PurchaseOrderLineResponse(
    val id: java.util.UUID,
    val lineNumber: Int,
    val productId: java.util.UUID,
    val productSku: String,
    val productName: String,
    val quantity: BigDecimal,
    val unitCost: BigDecimal,
    val lineTotal: BigDecimal,
    val receivedQuantity: BigDecimal,
    val billedQuantity: BigDecimal,
    val description: String?,
)

data class PurchaseOrderResponse(
    val id: java.util.UUID,
    val poNumber: String,
    val vendorId: java.util.UUID,
    val vendorName: String,
    val warehouseId: java.util.UUID,
    val orderDate: String,
    val expectedDate: String?,
    val referenceNumber: String?,
    val organizationId: java.util.UUID,
    val status: PurchaseOrderStatus,
    val lines: List<PurchaseOrderLineResponse>,
    val totalAmount: BigDecimal,
    val createdBy: java.util.UUID,
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
                            receivedQuantity = line.receivedQuantity,
                            billedQuantity = line.billedQuantity,
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
