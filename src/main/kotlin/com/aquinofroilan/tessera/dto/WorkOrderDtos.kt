package com.aquinofroilan.tessera.dto

import com.aquinofroilan.tessera.model.WorkOrder
import com.aquinofroilan.tessera.model.WorkOrderComponent
import com.aquinofroilan.tessera.model.WorkOrderOperation
import com.aquinofroilan.tessera.model.WorkOrderOperationStatus
import com.aquinofroilan.tessera.model.WorkOrderStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.LocalDate

data class CreateWorkOrderRequest(
    @field:NotBlank(message = "Product ID is required")
    val productId: String,
    val bomId: String? = null,
    val routingId: String? = null,
    @field:NotNull(message = "Quantity is required")
    @field:Positive(message = "Quantity must be positive")
    val quantity: BigDecimal?,
    @field:NotBlank(message = "Source warehouse is required")
    val sourceWarehouseId: String,
    @field:NotBlank(message = "Target warehouse is required")
    val targetWarehouseId: String,
    val plannedStart: LocalDate? = null,
    val plannedEnd: LocalDate? = null,
    val notes: String? = null,
)

data class WorkOrderComponentResponse(
    val id: String,
    val lineNumber: Int,
    val componentProductId: String,
    val componentSku: String,
    val componentName: String,
    val plannedQuantity: BigDecimal,
    val issuedQuantity: BigDecimal,
    val issuedCost: BigDecimal,
    val uom: String?,
    val scrapPct: BigDecimal,
) {
    companion object {
        fun from(c: WorkOrderComponent) =
            WorkOrderComponentResponse(
                id = c.id,
                lineNumber = c.lineNumber,
                componentProductId = c.componentProductId,
                componentSku = c.componentSku,
                componentName = c.componentName,
                plannedQuantity = c.plannedQuantity,
                issuedQuantity = c.issuedQuantity,
                issuedCost = c.issuedCost,
                uom = c.uom,
                scrapPct = c.scrapPct,
            )
    }
}

data class WorkOrderOperationResponse(
    val id: String,
    val operationNumber: Int,
    val workCenterId: String,
    val workCenterCode: String,
    val description: String,
    val plannedSetupMinutes: BigDecimal,
    val plannedRunMinutesPerUnit: BigDecimal,
    val actualMinutes: BigDecimal,
    val status: WorkOrderOperationStatus,
) {
    companion object {
        fun from(o: WorkOrderOperation) =
            WorkOrderOperationResponse(
                id = o.id,
                operationNumber = o.operationNumber,
                workCenterId = o.workCenterId,
                workCenterCode = o.workCenterCode,
                description = o.description,
                plannedSetupMinutes = o.plannedSetupMinutes,
                plannedRunMinutesPerUnit = o.plannedRunMinutesPerUnit,
                actualMinutes = o.actualMinutes,
                status = o.status,
            )
    }
}

data class WorkOrderResponse(
    val id: String,
    val woNumber: String,
    val productId: String,
    val productSku: String,
    val productName: String,
    val bomId: String,
    val routingId: String?,
    val quantity: BigDecimal,
    val quantityCompleted: BigDecimal,
    val quantityScrapped: BigDecimal,
    val totalIssuedCost: BigDecimal,
    val totalCompletedCost: BigDecimal,
    val sourceWarehouseId: String,
    val targetWarehouseId: String,
    val status: WorkOrderStatus,
    val plannedStart: LocalDate?,
    val plannedEnd: LocalDate?,
    val notes: String?,
    val components: List<WorkOrderComponentResponse>,
    val operations: List<WorkOrderOperationResponse>,
) {
    companion object {
        fun from(wo: WorkOrder) =
            WorkOrderResponse(
                id = wo.id,
                woNumber = wo.woNumber,
                productId = wo.productId,
                productSku = wo.productSku,
                productName = wo.productName,
                bomId = wo.bomId,
                routingId = wo.routingId,
                quantity = wo.quantity,
                quantityCompleted = wo.quantityCompleted,
                quantityScrapped = wo.quantityScrapped,
                totalIssuedCost = wo.totalIssuedCost,
                totalCompletedCost = wo.totalCompletedCost,
                sourceWarehouseId = wo.sourceWarehouseId,
                targetWarehouseId = wo.targetWarehouseId,
                status = wo.status,
                plannedStart = wo.plannedStart,
                plannedEnd = wo.plannedEnd,
                notes = wo.notes,
                components = wo.components.map { WorkOrderComponentResponse.from(it) },
                operations = wo.operations.map { WorkOrderOperationResponse.from(it) },
            )
    }
}
