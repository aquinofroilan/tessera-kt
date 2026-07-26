package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CompleteWorkOrderRequest
import com.aquinofroilan.tessera.dto.CreateStockMovementRequest
import com.aquinofroilan.tessera.dto.IssueMaterialRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.model.StockMovementType
import com.aquinofroilan.tessera.model.WorkOrder
import com.aquinofroilan.tessera.model.WorkOrderStatus
import com.aquinofroilan.tessera.repository.WorkOrderRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

/**
 * Drives work-order execution: material consumption (WIP_ISSUE) and
 * production completion (WIP_RECEIPT). Stock movements are created via
 * [StockMovementService.createMovementCapturingCost] so the WIP balance
 * can be tracked on the work-order itself for unit-cost derivation at
 * completion time.
 */
@Service
class WorkOrderExecutionService(
    private val workOrderRepository: WorkOrderRepository,
    private val workOrderService: WorkOrderService,
    private val stockMovementService: StockMovementService,
) {
    @Transactional
    fun issueMaterial(
        workOrderId: String,
        request: IssueMaterialRequest,
        organizationId: String,
        userId: String,
    ): WorkOrder {
        val wo = workOrderService.getWorkOrder(workOrderId, organizationId)
        requireActionable(wo, "issue material")

        val byLineId = wo.components.associateBy { it.id }
        var batchCost = BigDecimal.ZERO
        val updatedComponents =
            wo.components.toMutableList().also { mutable ->
                request.lines.forEach { lineReq ->
                    val component =
                        byLineId[lineReq.componentLineId]
                            ?: throw BusinessRuleException("Component line not found on this WO: ${lineReq.componentLineId}")
                    val qty = lineReq.quantity ?: throw BusinessRuleException("Quantity is required")
                    val remaining = component.plannedQuantity.subtract(component.issuedQuantity)
                    if (qty > remaining) {
                        throw BusinessRuleException(
                            "Cannot issue $qty of '${component.componentSku}'; only $remaining remains planned",
                        )
                    }
                    val (_, cost) =
                        stockMovementService.createMovementCapturingCost(
                            CreateStockMovementRequest(
                                type = StockMovementType.WIP_ISSUE,
                                productId = java.util.UUID.fromString(component.componentProductId),
                                warehouseId = java.util.UUID.fromString(wo.sourceWarehouseId),
                                quantity = qty,
                                reference = "WO-${wo.woNumber}-ISSUE",
                                notes = request.notes,
                            ),
                            java.util.UUID.fromString(organizationId),
                            java.util.UUID.fromString(userId),
                        )
                    val idx = mutable.indexOfFirst { it.id == component.id }
                    mutable[idx] =
                        component.copy(
                            issuedQuantity = component.issuedQuantity.add(qty),
                            issuedCost = component.issuedCost.add(cost),
                        )
                    batchCost = batchCost.add(cost)
                }
            }

        val nextStatus =
            if (wo.status == WorkOrderStatus.RELEASED) WorkOrderStatus.IN_PROGRESS else wo.status
        return workOrderRepository.save(
            wo.copy(
                status = nextStatus,
                startedAt = wo.startedAt ?: LocalDateTime.now(),
                totalIssuedCost = wo.totalIssuedCost.add(batchCost),
                components = updatedComponents,
            ),
        )
    }

    @Transactional
    fun completeProduction(
        workOrderId: String,
        request: CompleteWorkOrderRequest,
        organizationId: String,
        userId: String,
    ): WorkOrder {
        val wo = workOrderService.getWorkOrder(workOrderId, organizationId)
        requireActionable(wo, "complete production")

        val completed = request.quantityCompleted ?: throw BusinessRuleException("quantityCompleted is required")
        val scrapped = request.quantityScrapped ?: BigDecimal.ZERO
        if (completed.signum() == 0 && scrapped.signum() == 0) {
            throw BusinessRuleException("At least one of quantityCompleted or quantityScrapped must be positive")
        }
        val newCompleted = wo.quantityCompleted.add(completed)
        val newScrapped = wo.quantityScrapped.add(scrapped)
        if (newCompleted.add(newScrapped) > wo.quantity) {
            throw BusinessRuleException(
                "Completing $completed (+ scrap $scrapped) would exceed the planned quantity of ${wo.quantity}",
            )
        }

        var receiptCost = BigDecimal.ZERO
        if (completed.signum() > 0) {
            val unitCost = resolveUnitCost(wo, completed, request.unitCostOverride)
            val (_, cost) =
                stockMovementService.createMovementCapturingCost(
                    CreateStockMovementRequest(
                        type = StockMovementType.WIP_RECEIPT,
                        productId = java.util.UUID.fromString(wo.productId),
                        warehouseId = java.util.UUID.fromString(wo.targetWarehouseId),
                        quantity = completed,
                        unitCost = unitCost,
                        reference = "WO-${wo.woNumber}-COMPLETE",
                        notes = request.notes,
                    ),
                    java.util.UUID.fromString(organizationId),
                    java.util.UUID.fromString(userId),
                )
            receiptCost = cost
        }

        val finished = newCompleted.add(newScrapped).compareTo(wo.quantity) == 0
        return workOrderRepository.save(
            wo.copy(
                quantityCompleted = newCompleted,
                quantityScrapped = newScrapped,
                totalCompletedCost = wo.totalCompletedCost.add(receiptCost),
                status = if (finished) WorkOrderStatus.COMPLETED else WorkOrderStatus.IN_PROGRESS,
                startedAt = wo.startedAt ?: LocalDateTime.now(),
                completedAt = if (finished) LocalDateTime.now() else wo.completedAt,
                completedBy = if (finished) userId else wo.completedBy,
            ),
        )
    }

    private fun requireActionable(
        wo: WorkOrder,
        action: String,
    ) {
        if (wo.status != WorkOrderStatus.RELEASED && wo.status != WorkOrderStatus.IN_PROGRESS) {
            throw BusinessRuleException("Cannot $action on a ${wo.status} work order")
        }
    }

    private fun resolveUnitCost(
        wo: WorkOrder,
        completedNow: BigDecimal,
        override: BigDecimal?,
    ): BigDecimal {
        if (override != null) return override
        val outstandingWipCost = wo.totalIssuedCost.subtract(wo.totalCompletedCost)
        val outstandingPlannedOutput = wo.quantity.subtract(wo.quantityCompleted).subtract(wo.quantityScrapped)
        // Apportion remaining WIP cost across remaining planned output. Scrap absorbs into good output cost.
        if (outstandingPlannedOutput.signum() <= 0 || outstandingWipCost.signum() <= 0) {
            return BigDecimal.ZERO
        }
        val perUnit = outstandingWipCost.divide(outstandingPlannedOutput, 6, RoundingMode.HALF_UP)
        return perUnit
    }
}
