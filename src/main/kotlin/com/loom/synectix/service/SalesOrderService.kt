package com.loom.synectix.service

import com.loom.synectix.dto.CreateSalesOrderRequest
import com.loom.synectix.dto.CreateStockMovementRequest
import com.loom.synectix.exception.BusinessRuleException
import com.loom.synectix.exception.ResourceNotFoundException
import com.loom.synectix.model.SalesOrder
import com.loom.synectix.model.SalesOrderLine
import com.loom.synectix.model.SalesOrderStatus
import com.loom.synectix.model.StockMovementType
import com.loom.synectix.repository.SalesOrderRepository
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class SalesOrderService(
    private val salesOrderRepository: SalesOrderRepository,
    private val customerService: CustomerService,
    private val warehouseService: WarehouseService,
    private val productService: ProductService,
    private val stockMovementService: StockMovementService,
) {
    @Transactional
    fun createSalesOrder(
        request: CreateSalesOrderRequest,
        organizationId: String,
        createdBy: String,
    ): SalesOrder {
        val customer = customerService.getCustomer(request.customerId, organizationId)
        if (!customer.isActive) {
            throw BusinessRuleException("Cannot create a sales order for an inactive customer")
        }
        val warehouse = warehouseService.getWarehouse(request.warehouseId, organizationId)
        if (!warehouse.isActive) {
            throw BusinessRuleException("Cannot create a sales order for an inactive warehouse")
        }

        val lines =
            request.lines.mapIndexed { index, lineReq ->
                val quantity = lineReq.quantity ?: throw BusinessRuleException("Quantity is required")
                val unitPrice = lineReq.unitPrice ?: throw BusinessRuleException("Unit price is required")
                if (quantity.signum() <= 0) {
                    throw BusinessRuleException("Line quantity must be positive")
                }
                if (unitPrice.signum() < 0) {
                    throw BusinessRuleException("Line unit price must not be negative")
                }
                val product = productService.getProduct(lineReq.productId, organizationId)
                if (!product.isActive) {
                    throw BusinessRuleException("Product '${product.sku}' is inactive")
                }
                SalesOrderLine(
                    lineNumber = index + 1,
                    productId = product.id,
                    productSku = product.sku,
                    productName = product.name,
                    quantity = quantity,
                    unitPrice = unitPrice,
                    lineTotal = quantity.multiply(unitPrice),
                    description = lineReq.description,
                )
            }

        val total = lines.fold(BigDecimal.ZERO) { sum, line -> sum.add(line.lineTotal) }

        return saveWithRetry(organizationId) { number ->
            SalesOrder(
                soNumber = number,
                customerId = customer.id,
                customerName = customer.name,
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

    fun getSalesOrder(
        id: String,
        organizationId: String,
    ): SalesOrder {
        val so =
            salesOrderRepository.findById(id).orElseThrow {
                ResourceNotFoundException("Sales order not found")
            }
        if (so.organizationId != organizationId) {
            throw ResourceNotFoundException("Sales order not found")
        }
        return so
    }

    fun listSalesOrders(
        organizationId: String,
        status: SalesOrderStatus? = null,
        customerId: String? = null,
    ): List<SalesOrder> =
        when {
            status != null && customerId != null ->
                salesOrderRepository.findByOrganizationIdAndStatusAndCustomerId(organizationId, status, customerId)
            status != null -> salesOrderRepository.findByOrganizationIdAndStatus(organizationId, status)
            customerId != null -> salesOrderRepository.findByOrganizationIdAndCustomerId(organizationId, customerId)
            else -> salesOrderRepository.findByOrganizationId(organizationId)
        }

    @Transactional
    fun approveSalesOrder(
        id: String,
        organizationId: String,
        approvedBy: String,
    ): SalesOrder {
        val so = getSalesOrder(id, organizationId)
        if (so.status != SalesOrderStatus.DRAFT) {
            throw BusinessRuleException("Only draft sales orders can be approved")
        }
        return salesOrderRepository.save(
            so.copy(
                status = SalesOrderStatus.APPROVED,
                approvedAt = LocalDateTime.now(ZoneOffset.UTC),
                approvedBy = approvedBy,
            ),
        )
    }

    @Transactional
    fun fulfillSalesOrder(
        id: String,
        organizationId: String,
        userId: String,
    ): SalesOrder {
        val so = getSalesOrder(id, organizationId)
        if (so.status != SalesOrderStatus.APPROVED) {
            throw BusinessRuleException("Only approved sales orders can be fulfilled")
        }

        so.lines.forEach { line ->
            stockMovementService.createMovement(
                CreateStockMovementRequest(
                    type = StockMovementType.ISSUE,
                    productId = line.productId,
                    warehouseId = so.warehouseId,
                    quantity = line.quantity,
                    reference = "SO-${so.soNumber}",
                ),
                organizationId,
                userId,
            )
        }

        return salesOrderRepository.save(
            so.copy(
                status = SalesOrderStatus.FULFILLED,
                fulfilledAt = LocalDateTime.now(ZoneOffset.UTC),
            ),
        )
    }

    @Transactional
    fun closeSalesOrder(
        id: String,
        organizationId: String,
    ): SalesOrder {
        val so = getSalesOrder(id, organizationId)
        if (so.status != SalesOrderStatus.FULFILLED) {
            throw BusinessRuleException("Only fulfilled sales orders can be closed")
        }
        return salesOrderRepository.save(so.copy(status = SalesOrderStatus.CLOSED))
    }

    @Transactional
    fun cancelSalesOrder(
        id: String,
        organizationId: String,
    ): SalesOrder {
        val so = getSalesOrder(id, organizationId)
        if (so.status != SalesOrderStatus.DRAFT && so.status != SalesOrderStatus.APPROVED) {
            throw BusinessRuleException("Only draft or approved sales orders can be cancelled")
        }
        return salesOrderRepository.save(
            so.copy(
                status = SalesOrderStatus.CANCELLED,
                cancelledAt = LocalDateTime.now(ZoneOffset.UTC),
            ),
        )
    }

    private fun saveWithRetry(
        organizationId: String,
        maxRetries: Int = 3,
        build: (String) -> SalesOrder,
    ): SalesOrder {
        repeat(maxRetries) { attempt ->
            val count = salesOrderRepository.countByOrganizationId(organizationId)
            val number = "SO-${(count + 1).toString().padStart(4, '0')}"
            try {
                return salesOrderRepository.save(build(number))
            } catch (e: DuplicateKeyException) {
                if (attempt == maxRetries - 1) {
                    throw IllegalStateException("Failed to generate unique SO number: $number", e)
                }
            }
        }
        throw IllegalStateException("Failed to generate unique SO number")
    }
}
