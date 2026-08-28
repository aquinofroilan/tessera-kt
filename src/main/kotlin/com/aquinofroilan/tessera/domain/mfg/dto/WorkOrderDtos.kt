package com.aquinofroilan.tessera.domain.mfg.dto

import com.aquinofroilan.tessera.domain.mfg.model.WorkOrder
import com.aquinofroilan.tessera.domain.mfg.model.WorkOrderComponent
import com.aquinofroilan.tessera.domain.mfg.model.WorkOrderOperation
import com.aquinofroilan.tessera.domain.mfg.model.WorkOrderOperationStatus
import com.aquinofroilan.tessera.domain.mfg.model.WorkOrderStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.LocalDate

data class CreateWorkOrderRequest(
    @field:NotBlank(message = "Product ID is required")
    val productId: java.util.UUID,
    val bomId: java.util.UUID? = null,
    val routingId: java.util.UUID? = null,
    @field:NotNull(message = "Quantity is required")
    @field:Positive(message = "Quantity must be positive")
    val quantity: BigDecimal?,
    @field:NotBlank(message = "Source warehouse is required")
    val sourceWarehouseId: java.util.UUID,
    @field:NotBlank(message = "Target warehouse is required")
    val targetWarehouseId: java.util.UUID,
    val plannedStart: LocalDate? = null,
    val plannedEnd: LocalDate? = null,
    val notes: String? = null,
)

data class WorkOrderComponentResponse(
    val id: java.util.UUID,
    val lineNumber: Int,
    val componentProductId: java.util.UUID,
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
    val id: java.util.UUID,
    val operationNumber: Int,
    val workCenterId: java.util.UUID,
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
    val id: java.util.UUID,
    val woNumber: String,
    val productId: java.util.UUID,
    val productSku: String,
    val productName: String,
    val bomId: java.util.UUID,
    val routingId: java.util.UUID?,
    val quantity: BigDecimal,
    val quantityCompleted: BigDecimal,
    val quantityScrapped: BigDecimal,
    val totalIssuedCost: BigDecimal,
    val totalCompletedCost: BigDecimal,
    val sourceWarehouseId: java.util.UUID,
    val targetWarehouseId: java.util.UUID,
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
