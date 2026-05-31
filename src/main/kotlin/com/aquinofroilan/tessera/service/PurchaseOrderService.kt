package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreatePurchaseOrderRequest
import com.aquinofroilan.tessera.dto.CreateStockMovementRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.PurchaseOrder
import com.aquinofroilan.tessera.model.PurchaseOrderLine
import com.aquinofroilan.tessera.model.PurchaseOrderStatus
import com.aquinofroilan.tessera.model.StockMovementType
import com.aquinofroilan.tessera.repository.PurchaseOrderRepository
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class PurchaseOrderService(
    private val purchaseOrderRepository: PurchaseOrderRepository,
    private val vendorService: VendorService,
    private val warehouseService: WarehouseService,
    private val productService: ProductService,
    private val stockMovementService: StockMovementService,
) {
    @Transactional
    fun createPurchaseOrder(
        request: CreatePurchaseOrderRequest,
        organizationId: String,
        createdBy: String,
    ): PurchaseOrder {
        val vendor = vendorService.getVendor(request.vendorId, organizationId)
        if (!vendor.isActive) {
            throw BusinessRuleException("Cannot create a purchase order for an inactive vendor")
        }
        val warehouse = warehouseService.getWarehouse(request.warehouseId, organizationId)
        if (!warehouse.isActive) {
            throw BusinessRuleException("Cannot create a purchase order for an inactive warehouse")
        }

        val lines =
            request.lines.mapIndexed { index, lineReq ->
                val quantity = lineReq.quantity ?: throw BusinessRuleException("Quantity is required")
                val unitCost = lineReq.unitCost ?: throw BusinessRuleException("Unit cost is required")
                if (quantity.signum() <= 0) {
                    throw BusinessRuleException("Line quantity must be positive")
                }
                if (unitCost.signum() < 0) {
                    throw BusinessRuleException("Line unit cost must not be negative")
                }
                val product = productService.getProduct(lineReq.productId, organizationId)
                if (!product.isActive) {
                    throw BusinessRuleException("Product '${product.sku}' is inactive")
                }
                PurchaseOrderLine(
                    lineNumber = index + 1,
                    productId = product.id,
                    productSku = product.sku,
                    productName = product.name,
                    quantity = quantity,
                    unitCost = unitCost,
                    lineTotal = quantity.multiply(unitCost),
                    description = lineReq.description,
                )
            }

        val total = lines.fold(BigDecimal.ZERO) { sum, line -> sum.add(line.lineTotal) }

        return saveWithRetry(organizationId) { number ->
            PurchaseOrder(
                poNumber = number,
                vendorId = vendor.id,
                vendorName = vendor.name,
                warehouseId = warehouse.id,
                orderDate = request.orderDate ?: throw BusinessRuleException("Order date is required"),
                expectedDate = request.expectedDate,
                referenceNumber = request.referenceNumber,
                organizationId = organizationId,
                lines = lines,
                totalAmount = total,
                createdBy = createdBy,
            )
        }
    }

    fun getPurchaseOrder(
        id: String,
        organizationId: String,
    ): PurchaseOrder {
        val po =
            purchaseOrderRepository.findById(id).orElseThrow {
                ResourceNotFoundException("Purchase order not found")
            }
        if (po.organizationId != organizationId) {
            throw ResourceNotFoundException("Purchase order not found")
        }
        return po
    }

    fun listPurchaseOrders(
        organizationId: String,
        status: PurchaseOrderStatus? = null,
        vendorId: String? = null,
    ): List<PurchaseOrder> =
        when {
            status != null && vendorId != null ->
                purchaseOrderRepository.findByOrganizationIdAndStatusAndVendorId(organizationId, status, vendorId)
            status != null -> purchaseOrderRepository.findByOrganizationIdAndStatus(organizationId, status)
            vendorId != null -> purchaseOrderRepository.findByOrganizationIdAndVendorId(organizationId, vendorId)
            else -> purchaseOrderRepository.findByOrganizationId(organizationId)
        }

    @Transactional
    fun approvePurchaseOrder(
        id: String,
        organizationId: String,
        approvedBy: String,
    ): PurchaseOrder {
        val po = getPurchaseOrder(id, organizationId)
        if (po.status != PurchaseOrderStatus.DRAFT) {
            throw BusinessRuleException("Only draft purchase orders can be approved")
        }
        return purchaseOrderRepository.save(
            po.copy(
                status = PurchaseOrderStatus.APPROVED,
                approvedAt = LocalDateTime.now(ZoneOffset.UTC),
                approvedBy = approvedBy,
            ),
        )
    }

    @Transactional
    fun receivePurchaseOrder(
        id: String,
        organizationId: String,
        userId: String,
    ): PurchaseOrder {
        val po = getPurchaseOrder(id, organizationId)
        if (po.status != PurchaseOrderStatus.APPROVED) {
            throw BusinessRuleException("Only approved purchase orders can be received")
        }

        po.lines.forEach { line ->
            stockMovementService.createMovement(
                CreateStockMovementRequest(
                    type = StockMovementType.RECEIPT,
                    productId = line.productId,
                    warehouseId = po.warehouseId,
                    quantity = line.quantity,
                    unitCost = line.unitCost,
                    reference = "PO-${po.poNumber}",
                ),
                organizationId,
                userId,
            )
        }

        return purchaseOrderRepository.save(
            po.copy(
                status = PurchaseOrderStatus.RECEIVED,
                receivedAt = LocalDateTime.now(ZoneOffset.UTC),
            ),
        )
    }

    @Transactional
    fun closePurchaseOrder(
        id: String,
        organizationId: String,
    ): PurchaseOrder {
        val po = getPurchaseOrder(id, organizationId)
        if (po.status != PurchaseOrderStatus.RECEIVED) {
            throw BusinessRuleException("Only received purchase orders can be closed")
        }
        return purchaseOrderRepository.save(po.copy(status = PurchaseOrderStatus.CLOSED))
    }

    @Transactional
    fun cancelPurchaseOrder(
        id: String,
        organizationId: String,
    ): PurchaseOrder {
        val po = getPurchaseOrder(id, organizationId)
        if (po.status != PurchaseOrderStatus.DRAFT && po.status != PurchaseOrderStatus.APPROVED) {
            throw BusinessRuleException("Only draft or approved purchase orders can be cancelled")
        }
        return purchaseOrderRepository.save(
            po.copy(
                status = PurchaseOrderStatus.CANCELLED,
                cancelledAt = LocalDateTime.now(ZoneOffset.UTC),
            ),
        )
    }

    private fun saveWithRetry(
        organizationId: String,
        maxRetries: Int = 3,
        build: (String) -> PurchaseOrder,
    ): PurchaseOrder {
        repeat(maxRetries) { attempt ->
            val count = purchaseOrderRepository.countByOrganizationId(organizationId)
            val number = "PO-${(count + 1).toString().padStart(4, '0')}"
            try {
                return purchaseOrderRepository.save(build(number))
            } catch (e: DuplicateKeyException) {
                if (attempt == maxRetries - 1) {
                    throw IllegalStateException("Failed to generate unique PO number: $number", e)
                }
            }
        }
        throw IllegalStateException("Failed to generate unique PO number")
    }
}
