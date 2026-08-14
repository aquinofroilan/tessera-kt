package com.aquinofroilan.tessera.domain.sales.service

import com.aquinofroilan.tessera.domain.finance.dto.CreateInvoiceRequest
import com.aquinofroilan.tessera.domain.finance.dto.InvoiceLineRequest
import com.aquinofroilan.tessera.domain.finance.model.Invoice
import com.aquinofroilan.tessera.domain.finance.service.InvoiceService
import com.aquinofroilan.tessera.domain.inventory.dto.CreateStockMovementRequest
import com.aquinofroilan.tessera.domain.inventory.model.StockMovementType
import com.aquinofroilan.tessera.domain.inventory.service.ProductService
import com.aquinofroilan.tessera.domain.inventory.service.StockMovementService
import com.aquinofroilan.tessera.domain.inventory.service.WarehouseService
import com.aquinofroilan.tessera.domain.sales.dto.CreateSalesOrderRequest
import com.aquinofroilan.tessera.domain.sales.dto.FulfillSalesOrderRequest
import com.aquinofroilan.tessera.domain.sales.dto.GenerateInvoiceRequest
import com.aquinofroilan.tessera.domain.sales.model.SalesOrder
import com.aquinofroilan.tessera.domain.sales.model.SalesOrderLine
import com.aquinofroilan.tessera.domain.sales.model.SalesOrderStatus
import com.aquinofroilan.tessera.domain.sales.repository.SalesOrderRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class SalesOrderService(
    private val salesOrderRepository: SalesOrderRepository,
    private val customerService: CustomerService,
    private val warehouseService: WarehouseService,
    private val productService: ProductService,
    private val stockMovementService: StockMovementService,
    private val invoiceService: InvoiceService,
) {
    @Transactional
    fun createSalesOrder(
        request: CreateSalesOrderRequest,
        organizationId: java.util.UUID,
        createdBy: java.util.UUID,
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
        id: java.util.UUID,
        organizationId: java.util.UUID,
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
        organizationId: java.util.UUID,
        status: SalesOrderStatus? = null,
        customerId: java.util.UUID? = null,
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
        id: java.util.UUID,
        organizationId: java.util.UUID,
        approvedBy: java.util.UUID,
    ): SalesOrder {
        val so = getSalesOrder(id, organizationId)
        if (so.status != SalesOrderStatus.DRAFT) {
            throw BusinessRuleException("Only draft sales orders can be approved")
        }
        so.status = SalesOrderStatus.APPROVED
        so.approvedAt = LocalDateTime.now(ZoneOffset.UTC)
        so.approvedBy = approvedBy
        return salesOrderRepository.save(so)
    }

    @Transactional
    fun fulfillSalesOrder(
        id: java.util.UUID,
        request: FulfillSalesOrderRequest?,
        organizationId: java.util.UUID,
        userId: java.util.UUID,
    ): SalesOrder {
        val so = getSalesOrder(id, organizationId)
        if (so.status != SalesOrderStatus.APPROVED && so.status != SalesOrderStatus.PARTIALLY_FULFILLED) {
            throw BusinessRuleException("Only approved or partially fulfilled sales orders can be fulfilled")
        }

        val byId = so.lines.associateBy { it.id }
        val requested =
            if (request?.lines.isNullOrEmpty()) {
                so.lines
                    .associate { it.id to it.quantity.subtract(it.fulfilledQuantity) }
                    .filterValues { it.signum() > 0 }
            } else {
                request.lines.associate { it.lineId to (it.quantity ?: throw BusinessRuleException("Quantity is required")) }
            }
        requested.keys.forEach { if (it !in byId) throw BusinessRuleException("Unknown sales order line '$it'") }
        if (requested.isEmpty()) {
            throw BusinessRuleException("Nothing left to fulfill on this sales order")
        }

        val updatedLines =
            so.lines.map { line ->
                val qty = requested[line.id] ?: return@map line
                if (qty.signum() <= 0) throw BusinessRuleException("Fulfill quantity must be positive")
                val remaining = line.quantity.subtract(line.fulfilledQuantity)
                if (qty > remaining) {
                    throw BusinessRuleException("Cannot fulfill $qty of '${line.productSku}'; only $remaining remaining")
                }
                stockMovementService.createMovement(
                    CreateStockMovementRequest(
                        type = StockMovementType.ISSUE,
                        productId = line.productId,
                        warehouseId = so.warehouseId,
                        quantity = qty,
                        reference = "SO-${so.soNumber}",
                    ),
                    organizationId,
                    userId,
                )
                line.apply { fulfilledQuantity = line.fulfilledQuantity.add(qty) }
            }

        val fullyFulfilled = updatedLines.all { it.fulfilledQuantity >= it.quantity }
        so.lines = updatedLines
        so.status = if (fullyFulfilled) SalesOrderStatus.FULFILLED else SalesOrderStatus.PARTIALLY_FULFILLED
        so.fulfilledAt = if (fullyFulfilled) LocalDateTime.now(ZoneOffset.UTC) else so.fulfilledAt
        return salesOrderRepository.save(so)
    }

    @Transactional
    fun generateInvoice(
        id: java.util.UUID,
        request: GenerateInvoiceRequest?,
        organizationId: java.util.UUID,
        createdBy: java.util.UUID,
    ): Invoice {
        val so = getSalesOrder(id, organizationId)
        if (so.status != SalesOrderStatus.PARTIALLY_FULFILLED && so.status != SalesOrderStatus.FULFILLED) {
            throw BusinessRuleException("Sales order must be fulfilled before invoicing")
        }
        val customer = customerService.getCustomer(so.customerId, organizationId)
        val revenueAccountId =
            customer.defaultRevenueAccountId
                ?: throw BusinessRuleException("Customer '${customer.name}' has no default revenue account")

        val byId = so.lines.associateBy { it.id }
        val requested =
            if (request?.lines.isNullOrEmpty()) {
                so.lines
                    .associate { it.id to (it.fulfilledQuantity.subtract(it.invoicedQuantity) to null as BigDecimal?) }
                    .filterValues { it.first.signum() > 0 }
            } else {
                request.lines.associate {
                    it.lineId to ((it.quantity ?: throw BusinessRuleException("Quantity is required")) to it.unitPrice)
                }
            }
        requested.keys.forEach { if (it !in byId) throw BusinessRuleException("Unknown sales order line '$it'") }
        if (requested.isEmpty()) {
            throw BusinessRuleException("Nothing left to invoice on this sales order")
        }

        val invoiceLines = mutableListOf<InvoiceLineRequest>()
        val updatedLines =
            so.lines.map { line ->
                val entry = requested[line.id] ?: return@map line
                val (qty, overridePrice) = entry
                if (qty.signum() <= 0) throw BusinessRuleException("Invoice quantity must be positive")
                val invoiceable = line.fulfilledQuantity.subtract(line.invoicedQuantity)
                if (qty > invoiceable) {
                    throw BusinessRuleException(
                        "Cannot invoice $qty of '${line.productSku}'; only $invoiceable fulfilled and uninvoiced",
                    )
                }
                val unitPrice = overridePrice ?: line.unitPrice
                invoiceLines.add(
                    InvoiceLineRequest(
                        accountId = revenueAccountId,
                        amount = qty.multiply(unitPrice),
                        description = "${line.productSku} - ${line.productName}",
                    ),
                )
                line.apply { invoicedQuantity = line.invoicedQuantity.add(qty) }
            }

        val today = LocalDate.now(ZoneOffset.UTC)
        val invoice =
            invoiceService.createInvoice(
                CreateInvoiceRequest(
                    customerId = so.customerId,
                    date = today,
                    dueDate = today.plusDays(DEFAULT_INVOICE_TERM_DAYS),
                    referenceNumber = "SO-${so.soNumber}",
                    lines = invoiceLines,
                ),
                organizationId,
                createdBy,
            )
        so.lines = updatedLines
        salesOrderRepository.save(so)
        return invoice
    }

    @Transactional
    fun closeSalesOrder(
        id: java.util.UUID,
        organizationId: java.util.UUID,
    ): SalesOrder {
        val so = getSalesOrder(id, organizationId)
        if (so.status != SalesOrderStatus.FULFILLED) {
            throw BusinessRuleException("Only fulfilled sales orders can be closed")
        }
        so.status = SalesOrderStatus.CLOSED
        return salesOrderRepository.save(so)
    }

    @Transactional
    fun cancelSalesOrder(
        id: java.util.UUID,
        organizationId: java.util.UUID,
        userId: java.util.UUID,
    ): SalesOrder {
        val so = getSalesOrder(id, organizationId)
        if (so.status == SalesOrderStatus.CANCELLED || so.status == SalesOrderStatus.CLOSED) {
            throw BusinessRuleException("Closed or cancelled sales orders cannot be cancelled")
        }
        if (so.lines.any { it.invoicedQuantity.signum() > 0 }) {
            throw BusinessRuleException("Cannot cancel a sales order that has generated invoices; void the invoices first")
        }

        val fulfilled = so.status == SalesOrderStatus.PARTIALLY_FULFILLED || so.status == SalesOrderStatus.FULFILLED
        if (fulfilled) {
            stockMovementService.reverseByReference("SO-${so.soNumber}", organizationId, userId)
        }

        so.lines = if (fulfilled) so.lines.map { it.apply { fulfilledQuantity = BigDecimal.ZERO } } else so.lines
        so.status = SalesOrderStatus.CANCELLED
        so.cancelledAt = LocalDateTime.now(ZoneOffset.UTC)
        return salesOrderRepository.save(so)
    }

    private fun saveWithRetry(
        organizationId: java.util.UUID,
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

    private companion object {
        const val DEFAULT_INVOICE_TERM_DAYS = 30L
    }
}
