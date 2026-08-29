package com.aquinofroilan.tessera.domain.mfg.service

import com.aquinofroilan.tessera.domain.inventory.dto.CreateStockMovementRequest
import com.aquinofroilan.tessera.domain.inventory.model.StockMovementType
import com.aquinofroilan.tessera.domain.inventory.repository.ProductRepository
import com.aquinofroilan.tessera.domain.inventory.service.StockMovementService
import com.aquinofroilan.tessera.domain.mfg.dto.CreateSubcontractOrderRequest
import com.aquinofroilan.tessera.domain.mfg.dto.DispatchSubcontractOrderRequest
import com.aquinofroilan.tessera.domain.mfg.dto.ReceiveSubcontractGoodsRequest
import com.aquinofroilan.tessera.domain.mfg.dto.SubcontractOrderResponse
import com.aquinofroilan.tessera.domain.mfg.model.SubcontractComponent
import com.aquinofroilan.tessera.domain.mfg.model.SubcontractOrder
import com.aquinofroilan.tessera.domain.mfg.model.SubcontractOrderStatus
import com.aquinofroilan.tessera.domain.mfg.model.WorkOrderOperationStatus
import com.aquinofroilan.tessera.domain.mfg.model.WorkOrderStatus
import com.aquinofroilan.tessera.domain.mfg.repository.SubcontractOrderRepository
import com.aquinofroilan.tessera.domain.mfg.repository.WorkOrderRepository
import com.aquinofroilan.tessera.domain.procurement.repository.VendorRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@Service
class SubcontractOrderService(
    private val subcontractOrderRepository: SubcontractOrderRepository,
    private val workOrderRepository: WorkOrderRepository,
    private val vendorRepository: VendorRepository,
    private val productRepository: ProductRepository,
    private val stockMovementService: StockMovementService,
) {
    @Transactional(readOnly = true)
    fun listSubcontractOrders(
        organizationId: UUID,
        workOrderId: UUID? = null,
        vendorId: UUID? = null,
        status: SubcontractOrderStatus? = null,
    ): List<SubcontractOrderResponse> {
        val orders =
            when {
                workOrderId != null ->
                    subcontractOrderRepository.findByOrganizationIdAndWorkOrderId(organizationId, workOrderId)
                vendorId != null ->
                    subcontractOrderRepository.findByOrganizationIdAndVendorId(organizationId, vendorId)
                status != null ->
                    subcontractOrderRepository.findByOrganizationIdAndStatus(organizationId, status)
                else ->
                    subcontractOrderRepository.findByOrganizationId(organizationId)
            }

        return orders.map { order ->
            val vendorName = vendorRepository.findById(order.vendorId).map { it.name }.orElse(null)
            SubcontractOrderResponse.from(order, vendorName)
        }
    }

    @Transactional(readOnly = true)
    fun getSubcontractOrder(
        id: UUID,
        organizationId: UUID,
    ): SubcontractOrderResponse {
        val order =
            subcontractOrderRepository.findByIdAndOrganizationId(id, organizationId).orElseThrow {
                ResourceNotFoundException("Subcontract order $id not found")
            }
        val vendorName = vendorRepository.findById(order.vendorId).map { it.name }.orElse(null)
        return SubcontractOrderResponse.from(order, vendorName)
    }

    @Transactional
    fun createSubcontractOrder(
        organizationId: UUID,
        userId: UUID,
        request: CreateSubcontractOrderRequest,
    ): SubcontractOrderResponse {
        val workOrder =
            workOrderRepository.findById(request.workOrderId).orElseThrow {
                ResourceNotFoundException("Work order ${request.workOrderId} not found")
            }
        if (workOrder.organizationId != organizationId) {
            throw ResourceNotFoundException("Work order ${request.workOrderId} not found")
        }
        if (workOrder.status == WorkOrderStatus.CLOSED || workOrder.status == WorkOrderStatus.CANCELLED) {
            throw BusinessRuleException("Cannot create subcontract order for a closed or cancelled work order")
        }

        val vendor =
            vendorRepository.findByIdAndOrganizationId(request.vendorId, organizationId).orElseThrow {
                ResourceNotFoundException("Vendor ${request.vendorId} not found")
            }

        val matchingOp = workOrder.operations.find { it.operationNumber == request.operationNumber }

        val count = subcontractOrderRepository.countByOrganizationId(organizationId)
        val orderNumber = "SCO-%05d".format(count + 1)

        val unitCost = request.unitServiceCost ?: BigDecimal.ZERO
        val totalCost = request.quantity.multiply(unitCost)

        val componentsList = mutableListOf<SubcontractComponent>()

        if (!request.components.isNullOrEmpty()) {
            request.components.forEach { compReq ->
                val product =
                    productRepository.findById(compReq.productId).orElseThrow {
                        ResourceNotFoundException("Product ${compReq.productId} not found")
                    }
                componentsList.add(
                    SubcontractComponent(
                        productId = product.id,
                        productSku = compReq.productSku ?: product.sku,
                        productName = compReq.productName ?: product.name,
                        plannedQuantity = compReq.plannedQuantity,
                        dispatchedQuantity = BigDecimal.ZERO,
                        uom = compReq.uom,
                    ),
                )
            }
        } else {
            // Auto-populate from WorkOrder components if available
            workOrder.components.forEach { woComp ->
                componentsList.add(
                    SubcontractComponent(
                        productId = woComp.componentProductId,
                        productSku = woComp.componentSku,
                        productName = woComp.componentName,
                        plannedQuantity = woComp.plannedQuantity,
                        dispatchedQuantity = BigDecimal.ZERO,
                        uom = woComp.uom,
                    ),
                )
            }
        }

        val subcontractOrder =
            SubcontractOrder(
                organizationId = organizationId,
                orderNumber = orderNumber,
                workOrderId = workOrder.id,
                operationId = request.operationId ?: matchingOp?.id,
                operationNumber = request.operationNumber,
                vendorId = vendor.id,
                serviceItemName = request.serviceItemName.trim(),
                quantity = request.quantity,
                receivedQuantity = BigDecimal.ZERO,
                unitServiceCost = unitCost,
                totalCost = totalCost,
                status = SubcontractOrderStatus.DRAFT,
                notes = request.notes?.trim(),
                components = componentsList,
                createdBy = userId,
            )

        val saved = subcontractOrderRepository.save(subcontractOrder)
        return SubcontractOrderResponse.from(saved, vendor.name)
    }

    @Transactional
    fun dispatchComponents(
        id: UUID,
        organizationId: UUID,
        userId: UUID,
        request: DispatchSubcontractOrderRequest,
    ): SubcontractOrderResponse {
        val order =
            subcontractOrderRepository.findByIdAndOrganizationId(id, organizationId).orElseThrow {
                ResourceNotFoundException("Subcontract order $id not found")
            }

        if (order.status != SubcontractOrderStatus.DRAFT && order.status != SubcontractOrderStatus.DISPATCHED) {
            throw BusinessRuleException("Cannot dispatch components for order in status ${order.status}")
        }

        val workOrder =
            workOrderRepository.findById(order.workOrderId).orElseThrow {
                ResourceNotFoundException("Work order ${order.workOrderId} not found")
            }

        if (request.items.isNullOrEmpty()) {
            // Dispatch all remaining planned components
            order.components.forEach { comp ->
                val remaining = comp.plannedQuantity.subtract(comp.dispatchedQuantity)
                if (remaining > BigDecimal.ZERO) {
                    stockMovementService.createMovementCapturingCost(
                        CreateStockMovementRequest(
                            type = StockMovementType.WIP_ISSUE,
                            productId = comp.productId,
                            warehouseId = workOrder.sourceWarehouseId,
                            quantity = remaining,
                            reference = "SCO-${order.orderNumber}-DISPATCH",
                            notes = request.notes ?: "Dispatched to subcontractor",
                        ),
                        organizationId,
                        userId,
                    )
                    comp.dispatchedQuantity = comp.plannedQuantity
                }
            }
        } else {
            val byId = order.components.associateBy { it.id }
            request.items.forEach { item ->
                val comp =
                    byId[item.componentId]
                        ?: throw BusinessRuleException("Component ${item.componentId} not found in subcontract order")
                val remaining = comp.plannedQuantity.subtract(comp.dispatchedQuantity)
                if (item.quantity > remaining) {
                    throw BusinessRuleException(
                        "Cannot dispatch ${item.quantity} of '${comp.productSku}'; only $remaining remaining planned",
                    )
                }
                stockMovementService.createMovementCapturingCost(
                    CreateStockMovementRequest(
                        type = StockMovementType.WIP_ISSUE,
                        productId = comp.productId,
                        warehouseId = workOrder.sourceWarehouseId,
                        quantity = item.quantity,
                        reference = "SCO-${order.orderNumber}-DISPATCH",
                        notes = request.notes ?: "Dispatched to subcontractor",
                    ),
                    organizationId,
                    userId,
                )
                comp.dispatchedQuantity = comp.dispatchedQuantity.add(item.quantity)
            }
        }

        order.status = SubcontractOrderStatus.DISPATCHED
        order.dispatchedAt = LocalDateTime.now(ZoneOffset.UTC)

        // Ensure WorkOrder is marked IN_PROGRESS
        if (workOrder.status == WorkOrderStatus.RELEASED) {
            workOrderRepository.save(
                workOrder.copy(
                    status = WorkOrderStatus.IN_PROGRESS,
                    startedAt = workOrder.startedAt ?: LocalDateTime.now(ZoneOffset.UTC),
                ),
            )
        }

        val saved = subcontractOrderRepository.save(order)
        val vendorName = vendorRepository.findById(saved.vendorId).map { it.name }.orElse(null)
        return SubcontractOrderResponse.from(saved, vendorName)
    }

    @Transactional
    fun receiveProcessedGoods(
        id: UUID,
        organizationId: UUID,
        userId: UUID,
        request: ReceiveSubcontractGoodsRequest,
    ): SubcontractOrderResponse {
        val order =
            subcontractOrderRepository.findByIdAndOrganizationId(id, organizationId).orElseThrow {
                ResourceNotFoundException("Subcontract order $id not found")
            }

        if (order.status != SubcontractOrderStatus.DISPATCHED && order.status != SubcontractOrderStatus.PARTIALLY_RECEIVED) {
            throw BusinessRuleException("Cannot receive goods for subcontract order in status ${order.status}")
        }

        val remainingPlanned = order.quantity.subtract(order.receivedQuantity)
        if (request.quantityReceived > remainingPlanned) {
            throw BusinessRuleException(
                "Receiving ${request.quantityReceived} exceeds remaining planned quantity of $remainingPlanned",
            )
        }

        val unitCost = request.unitServiceCostOverride ?: order.unitServiceCost
        val serviceCostAdded = request.quantityReceived.multiply(unitCost)

        order.receivedQuantity = order.receivedQuantity.add(request.quantityReceived)
        order.receivedAt = LocalDateTime.now(ZoneOffset.UTC)

        val isComplete = order.receivedQuantity.compareTo(order.quantity) >= 0
        if (isComplete) {
            order.status = SubcontractOrderStatus.COMPLETED
            order.completedAt = LocalDateTime.now(ZoneOffset.UTC)
        } else {
            order.status = SubcontractOrderStatus.PARTIALLY_RECEIVED
        }

        // Update Work Order operation and cost accumulation
        val workOrder =
            workOrderRepository.findById(order.workOrderId).orElseThrow {
                ResourceNotFoundException("Work order ${order.workOrderId} not found")
            }

        val updatedOperations =
            workOrder.operations.map { op ->
                if (op.operationNumber == order.operationNumber) {
                    val nextOpStatus =
                        if (isComplete) WorkOrderOperationStatus.DONE else WorkOrderOperationStatus.IN_PROGRESS
                    op.copy(status = nextOpStatus)
                } else {
                    op
                }
            }

        val updatedWo =
            workOrder.copy(
                totalIssuedCost = workOrder.totalIssuedCost.add(serviceCostAdded),
                operations = updatedOperations,
                status = if (workOrder.status == WorkOrderStatus.RELEASED) WorkOrderStatus.IN_PROGRESS else workOrder.status,
            )
        workOrderRepository.save(updatedWo)

        val saved = subcontractOrderRepository.save(order)
        val vendorName = vendorRepository.findById(saved.vendorId).map { it.name }.orElse(null)
        return SubcontractOrderResponse.from(saved, vendorName)
    }

    @Transactional
    fun cancelSubcontractOrder(
        id: UUID,
        organizationId: UUID,
        userId: UUID,
    ): SubcontractOrderResponse {
        val order =
            subcontractOrderRepository.findByIdAndOrganizationId(id, organizationId).orElseThrow {
                ResourceNotFoundException("Subcontract order $id not found")
            }

        if (order.status == SubcontractOrderStatus.COMPLETED) {
            throw BusinessRuleException("Cannot cancel a completed subcontract order")
        }
        if (order.status == SubcontractOrderStatus.CANCELLED) {
            throw BusinessRuleException("Subcontract order is already cancelled")
        }

        order.status = SubcontractOrderStatus.CANCELLED
        order.cancelledAt = LocalDateTime.now(ZoneOffset.UTC)

        val saved = subcontractOrderRepository.save(order)
        val vendorName = vendorRepository.findById(saved.vendorId).map { it.name }.orElse(null)
        return SubcontractOrderResponse.from(saved, vendorName)
    }
}
