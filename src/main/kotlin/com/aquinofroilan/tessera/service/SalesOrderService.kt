package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateInvoiceRequest
import com.aquinofroilan.tessera.dto.CreateSalesOrderRequest
import com.aquinofroilan.tessera.dto.CreateStockMovementRequest
import com.aquinofroilan.tessera.dto.FulfillSalesOrderRequest
import com.aquinofroilan.tessera.dto.GenerateInvoiceRequest
import com.aquinofroilan.tessera.dto.InvoiceLineRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.Invoice
import com.aquinofroilan.tessera.model.SalesOrder
import com.aquinofroilan.tessera.model.SalesOrderLine
import com.aquinofroilan.tessera.model.SalesOrderStatus
import com.aquinofroilan.tessera.model.StockMovementType
import com.aquinofroilan.tessera.repository.SalesOrderRepository
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
        request: FulfillSalesOrderRequest?,
        organizationId: String,
        userId: String,
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
                request.lines!!.associate { it.lineId to (it.quantity ?: throw BusinessRuleException("Quantity is required")) }
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
                line.copy(fulfilledQuantity = line.fulfilledQuantity.add(qty))
            }

        val fullyFulfilled = updatedLines.all { it.fulfilledQuantity >= it.quantity }
        return salesOrderRepository.save(
            so.copy(
                lines = updatedLines,
                status = if (fullyFulfilled) SalesOrderStatus.FULFILLED else SalesOrderStatus.PARTIALLY_FULFILLED,
                fulfilledAt = if (fullyFulfilled) LocalDateTime.now(ZoneOffset.UTC) else so.fulfilledAt,
            ),
        )
    }

    @Transactional
    fun generateInvoice(
        id: String,
        request: GenerateInvoiceRequest?,
        organizationId: String,
        createdBy: String,
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
                request.lines!!.associate {
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
                line.copy(invoicedQuantity = line.invoicedQuantity.add(qty))
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
        salesOrderRepository.save(so.copy(lines = updatedLines))
        return invoice
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

    private companion object {
        const val DEFAULT_INVOICE_TERM_DAYS = 30L
    }
}
