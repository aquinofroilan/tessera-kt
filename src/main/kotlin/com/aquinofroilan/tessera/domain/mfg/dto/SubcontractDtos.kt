package com.aquinofroilan.tessera.domain.mfg.dto

import com.aquinofroilan.tessera.domain.mfg.model.SubcontractComponent
import com.aquinofroilan.tessera.domain.mfg.model.SubcontractOrder
import com.aquinofroilan.tessera.domain.mfg.model.SubcontractOrderStatus
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class SubcontractComponentDto(
    val id: UUID,
    val productId: UUID,
    val productSku: String,
    val productName: String,
    val plannedQuantity: BigDecimal,
    val dispatchedQuantity: BigDecimal,
    val uom: String?,
) {
    companion object {
        fun from(entity: SubcontractComponent): SubcontractComponentDto =
            SubcontractComponentDto(
                id = entity.id,
                productId = entity.productId,
                productSku = entity.productSku,
                productName = entity.productName,
                plannedQuantity = entity.plannedQuantity,
                dispatchedQuantity = entity.dispatchedQuantity,
                uom = entity.uom,
            )
    }
}

data class SubcontractOrderResponse(
    val id: UUID,
    val organizationId: UUID,
    val orderNumber: String,
    val workOrderId: UUID,
    val operationId: UUID?,
    val operationNumber: Int,
    val vendorId: UUID,
    val vendorName: String?,
    val purchaseOrderId: UUID?,
    val serviceItemName: String,
    val quantity: BigDecimal,
    val receivedQuantity: BigDecimal,
    val unitServiceCost: BigDecimal,
    val totalCost: BigDecimal,
    val status: SubcontractOrderStatus,
    val dispatchedAt: LocalDateTime?,
    val receivedAt: LocalDateTime?,
    val completedAt: LocalDateTime?,
    val cancelledAt: LocalDateTime?,
    val notes: String?,
    val components: List<SubcontractComponentDto>,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
) {
    companion object {
        fun from(
            order: SubcontractOrder,
            vendorName: String? = null,
        ): SubcontractOrderResponse =
            SubcontractOrderResponse(
                id = order.id,
                organizationId = order.organizationId,
                orderNumber = order.orderNumber,
                workOrderId = order.workOrderId,
                operationId = order.operationId,
                operationNumber = order.operationNumber,
                vendorId = order.vendorId,
                vendorName = vendorName,
                purchaseOrderId = order.purchaseOrderId,
                serviceItemName = order.serviceItemName,
                quantity = order.quantity,
                receivedQuantity = order.receivedQuantity,
                unitServiceCost = order.unitServiceCost,
                totalCost = order.totalCost,
                status = order.status,
                dispatchedAt = order.dispatchedAt,
                receivedAt = order.receivedAt,
                completedAt = order.completedAt,
                cancelledAt = order.cancelledAt,
                notes = order.notes,
                components = order.components.map { SubcontractComponentDto.from(it) },
                createdAt = order.createdAt,
                updatedAt = order.updatedAt,
            )
    }
}

data class CreateSubcontractComponentRequest(
    @field:NotNull(message = "Product ID is required")
    val productId: UUID,
    val productSku: String? = null,
    val productName: String? = null,
    @field:NotNull(message = "Planned quantity is required")
    @field:DecimalMin(value = "0.0001", message = "Planned quantity must be greater than 0")
    val plannedQuantity: BigDecimal,
    val uom: String? = null,
)

data class CreateSubcontractOrderRequest(
    @field:NotNull(message = "Work Order ID is required")
    val workOrderId: UUID,
    val operationId: UUID? = null,
    @field:NotNull(message = "Operation number is required")
    val operationNumber: Int,
    @field:NotNull(message = "Vendor ID is required")
    val vendorId: UUID,
    @field:NotBlank(message = "Service item name is required")
    val serviceItemName: String,
    @field:NotNull(message = "Quantity is required")
    @field:DecimalMin(value = "0.0001", message = "Quantity must be greater than 0")
    val quantity: BigDecimal,
    val unitServiceCost: BigDecimal? = null,
    val notes: String? = null,
    val components: List<CreateSubcontractComponentRequest>? = null,
)

data class DispatchSubcontractComponentItem(
    @field:NotNull(message = "Component ID is required")
    val componentId: UUID,
    @field:NotNull(message = "Quantity is required")
    @field:DecimalMin(value = "0.0001", message = "Quantity must be greater than 0")
    val quantity: BigDecimal,
)

data class DispatchSubcontractOrderRequest(
    val items: List<DispatchSubcontractComponentItem>? = null,
    val notes: String? = null,
)

data class ReceiveSubcontractGoodsRequest(
    @field:NotNull(message = "Quantity received is required")
    @field:DecimalMin(value = "0.0001", message = "Quantity received must be greater than 0")
    val quantityReceived: BigDecimal,
    val unitServiceCostOverride: BigDecimal? = null,
    val notes: String? = null,
)
