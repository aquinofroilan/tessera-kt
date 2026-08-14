package com.aquinofroilan.tessera.domain.mfg.service

import com.aquinofroilan.tessera.domain.inventory.service.ProductService
import com.aquinofroilan.tessera.domain.inventory.service.WarehouseService
import com.aquinofroilan.tessera.domain.mfg.dto.CreateWorkOrderRequest
import com.aquinofroilan.tessera.domain.mfg.model.BillOfMaterials
import com.aquinofroilan.tessera.domain.mfg.model.BomStatus
import com.aquinofroilan.tessera.domain.mfg.model.Routing
import com.aquinofroilan.tessera.domain.mfg.model.RoutingStatus
import com.aquinofroilan.tessera.domain.mfg.model.WorkOrder
import com.aquinofroilan.tessera.domain.mfg.model.WorkOrderComponent
import com.aquinofroilan.tessera.domain.mfg.model.WorkOrderOperation
import com.aquinofroilan.tessera.domain.mfg.model.WorkOrderStatus
import com.aquinofroilan.tessera.domain.mfg.repository.WorkOrderRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

@Service
class WorkOrderService(
    private val workOrderRepository: WorkOrderRepository,
    private val productService: ProductService,
    private val warehouseService: WarehouseService,
    private val bomService: BillOfMaterialsService,
    private val routingService: RoutingService,
) {
    @Transactional
    fun createWorkOrder(
        request: CreateWorkOrderRequest,
        organizationId: java.util.UUID,
        createdBy: String,
    ): WorkOrder {
        val product = productService.getProduct(request.productId, organizationId)
        if (!product.isActive) {
            throw BusinessRuleException("Product '${product.sku}' is inactive")
        }
        val quantity = request.quantity ?: throw BusinessRuleException("Quantity is required")
        if (quantity.signum() <= 0) throw BusinessRuleException("Quantity must be positive")

        val source =
            warehouseService.getWarehouse(
                request.sourceWarehouseId,
                organizationId,
            )
        if (!source.isActive) throw BusinessRuleException("Source warehouse is inactive")
        val target =
            warehouseService.getWarehouse(
                request.targetWarehouseId,
                organizationId,
            )
        if (!target.isActive) throw BusinessRuleException("Target warehouse is inactive")
        if (request.plannedEnd != null &&
            request.plannedStart != null &&
            request.plannedEnd.isBefore(request.plannedStart)
        ) {
            throw BusinessRuleException("Planned end must be on or after planned start")
        }

        val bom = resolveBom(request.bomId, organizationId, product.id)
        val routing =
            request.routingId?.let { resolveRouting(it, organizationId, product.id) }
                ?: resolveDefaultRouting(organizationId, product.id)

        val components =
            bom.lines.map { line ->
                WorkOrderComponent(
                    lineNumber = line.lineNumber,
                    componentProductId = line.componentProductId,
                    componentSku = line.componentSku,
                    componentName = line.componentName,
                    plannedQuantity = scaleComponentQuantity(line.quantity, line.scrapPct, quantity),
                    uom = line.uom,
                    scrapPct = line.scrapPct,
                )
            }
        val operations =
            routing?.operations?.map { op ->
                WorkOrderOperation(
                    operationNumber = op.operationNumber,
                    workCenterId = op.workCenterId,
                    workCenterCode = op.workCenterCode,
                    description = op.description,
                    plannedSetupMinutes = op.setupMinutes,
                    plannedRunMinutesPerUnit = op.runMinutesPerUnit,
                )
            } ?: emptyList()

        return saveWithGeneratedNumber(organizationId) { woNumber ->
            WorkOrder(
                organizationId = organizationId,
                woNumber = woNumber,
                productId = product.id,
                productSku = product.sku,
                productName = product.name,
                bomId = bom.id,
                routingId = routing?.id,
                quantity = quantity,
                sourceWarehouseId = source.id,
                targetWarehouseId = target.id,
                status = WorkOrderStatus.DRAFT,
                plannedStart = request.plannedStart,
                plannedEnd = request.plannedEnd,
                notes = request.notes,
                components = components,
                operations = operations,
                createdBy = createdBy,
            )
        }
    }

    fun getWorkOrder(
        id: java.util.UUID,
        organizationId: java.util.UUID,
    ): WorkOrder {
        val wo =
            workOrderRepository.findById(id).orElseThrow {
                ResourceNotFoundException("Work order not found: $id")
            }
        if (wo.organizationId != organizationId) {
            throw ResourceNotFoundException("Work order not found: $id")
        }
        return wo
    }

    fun listWorkOrders(
        organizationId: java.util.UUID,
        status: WorkOrderStatus?,
        productId: java.util.UUID?,
    ): List<WorkOrder> =
        when {
            status != null -> workOrderRepository.findByOrganizationIdAndStatus(organizationId, status)
            productId != null -> workOrderRepository.findByOrganizationIdAndProductId(organizationId, productId)
            else -> workOrderRepository.findByOrganizationId(organizationId)
        }

    @Transactional
    fun releaseWorkOrder(
        id: java.util.UUID,
        organizationId: java.util.UUID,
        userId: String,
    ): WorkOrder {
        val wo = getWorkOrder(id, organizationId)
        if (wo.status != WorkOrderStatus.DRAFT) {
            throw BusinessRuleException("Only DRAFT work orders can be released; this WO is ${wo.status}")
        }
        return workOrderRepository.save(
            wo.copy(
                status = WorkOrderStatus.RELEASED,
                releasedAt = LocalDateTime.now(),
                releasedBy = userId,
            ),
        )
    }

    @Transactional
    fun cancelWorkOrder(
        id: java.util.UUID,
        organizationId: java.util.UUID,
        userId: String,
    ): WorkOrder {
        val wo = getWorkOrder(id, organizationId)
        if (wo.status == WorkOrderStatus.COMPLETED || wo.status == WorkOrderStatus.CLOSED) {
            throw BusinessRuleException("Cannot cancel a ${wo.status} work order")
        }
        if (wo.status == WorkOrderStatus.CANCELLED) return wo
        return workOrderRepository.save(
            wo.copy(
                status = WorkOrderStatus.CANCELLED,
                cancelledAt = LocalDateTime.now(),
                cancelledBy = userId,
            ),
        )
    }

    private fun resolveBom(
        bomId: java.util.UUID?,
        organizationId: java.util.UUID,
        productId: java.util.UUID,
    ): com.aquinofroilan.tessera.domain.mfg.model.BillOfMaterials {
        val bom =
            if (bomId != null) {
                bomService.getBom(bomId, organizationId)
            } else {
                bomService
                    .listBoms(organizationId, BomStatus.ACTIVE, productId)
                    .firstOrNull { it.isDefault }
                    ?: throw BusinessRuleException("No default ACTIVE BOM exists for product $productId")
            }
        if (bom.productId != productId) {
            throw BusinessRuleException("BOM '${bom.code}' is not for product $productId")
        }
        if (bom.status != BomStatus.ACTIVE) {
            throw BusinessRuleException("BOM '${bom.code}' is ${bom.status}; only ACTIVE BOMs can be used")
        }
        return bom
    }

    private fun resolveRouting(
        routingId: java.util.UUID,
        organizationId: java.util.UUID,
        productId: java.util.UUID,
    ): com.aquinofroilan.tessera.domain.mfg.model.Routing {
        val routing = routingService.getRouting(routingId, organizationId)
        if (routing.productId != productId) {
            throw BusinessRuleException("Routing '${routing.code}' is not for product $productId")
        }
        if (routing.status != RoutingStatus.ACTIVE) {
            throw BusinessRuleException("Routing '${routing.code}' is ${routing.status}; only ACTIVE routings can be used")
        }
        return routing
    }

    private fun resolveDefaultRouting(
        organizationId: java.util.UUID,
        productId: java.util.UUID,
    ): com.aquinofroilan.tessera.domain.mfg.model.Routing? =
        routingService
            .listRoutings(organizationId, RoutingStatus.ACTIVE, productId)
            .firstOrNull { it.isDefault }

    private fun scaleComponentQuantity(
        perUnit: BigDecimal,
        scrapPct: BigDecimal,
        orderQuantity: BigDecimal,
    ): BigDecimal {
        val gross = perUnit.multiply(orderQuantity)
        if (scrapPct.signum() == 0) return gross
        val factor = BigDecimal.ONE.add(scrapPct.divide(BigDecimal(100), 6, RoundingMode.HALF_UP))
        return gross.multiply(factor).setScale(4, RoundingMode.HALF_UP)
    }

    private fun saveWithGeneratedNumber(
        organizationId: java.util.UUID,
        maxRetries: Int = 3,
        build: (String) -> WorkOrder,
    ): WorkOrder {
        repeat(maxRetries) { attempt ->
            val count = workOrderRepository.countByOrganizationId(organizationId)
            val number = "WO-${(count + 1).toString().padStart(4, '0')}"
            try {
                return workOrderRepository.save(build(number))
            } catch (e: DuplicateKeyException) {
                if (attempt == maxRetries - 1) {
                    throw IllegalStateException("Failed to generate unique WO number: $number", e)
                }
            }
        }
        throw IllegalStateException("Failed to generate unique WO number")
    }
}
