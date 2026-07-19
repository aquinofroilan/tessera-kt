package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.BillLineRequest
import com.aquinofroilan.tessera.dto.BillMatchLineResult
import com.aquinofroilan.tessera.dto.BillMatchRequest
import com.aquinofroilan.tessera.dto.BillMatchResult
import com.aquinofroilan.tessera.dto.CreateBillRequest
import com.aquinofroilan.tessera.dto.CreatePurchaseOrderRequest
import com.aquinofroilan.tessera.dto.CreateStockMovementRequest
import com.aquinofroilan.tessera.dto.GenerateBillRequest
import com.aquinofroilan.tessera.dto.MatchStatus
import com.aquinofroilan.tessera.dto.ReceivePurchaseOrderRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.Bill
import com.aquinofroilan.tessera.model.PurchaseOrder
import com.aquinofroilan.tessera.model.PurchaseOrderLine
import com.aquinofroilan.tessera.model.PurchaseOrderStatus
import com.aquinofroilan.tessera.model.StockMovementType
import com.aquinofroilan.tessera.repository.AccountRepository
import com.aquinofroilan.tessera.repository.PurchaseOrderRepository
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class PurchaseOrderService(
    private val purchaseOrderRepository: PurchaseOrderRepository,
    private val vendorService: VendorService,
    private val warehouseService: WarehouseService,
    private val productService: ProductService,
    private val stockMovementService: StockMovementService,
    private val accountRepository: AccountRepository,
    private val billService: BillService,
) {
    @Transactional
    fun createPurchaseOrder(
        request: CreatePurchaseOrderRequest,
        organizationId: java.util.UUID,
        createdBy: java.util.UUID,
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
        id: java.util.UUID,
        organizationId: java.util.UUID,
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
        organizationId: java.util.UUID,
        status: PurchaseOrderStatus? = null,
        vendorId: java.util.UUID? = null,
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
        id: java.util.UUID,
        organizationId: java.util.UUID,
        approvedBy: java.util.UUID,
    ): PurchaseOrder {
        val po = getPurchaseOrder(id, organizationId)
        if (po.status != PurchaseOrderStatus.DRAFT) {
            throw BusinessRuleException("Only draft purchase orders can be approved")
        }
        po.status = PurchaseOrderStatus.APPROVED
        po.approvedAt = LocalDateTime.now(ZoneOffset.UTC)
        po.approvedBy = approvedBy
        return purchaseOrderRepository.save(po)
    }

    @Transactional
    fun receivePurchaseOrder(
        id: java.util.UUID,
        request: ReceivePurchaseOrderRequest?,
        organizationId: java.util.UUID,
        userId: java.util.UUID,
    ): PurchaseOrder {
        val po = getPurchaseOrder(id, organizationId)
        if (po.status != PurchaseOrderStatus.APPROVED && po.status != PurchaseOrderStatus.PARTIALLY_RECEIVED) {
            throw BusinessRuleException("Only approved or partially received purchase orders can be received")
        }

        val byId = po.lines.associateBy { it.id }
        val requested =
            if (request?.lines.isNullOrEmpty()) {
                po.lines
                    .associate { it.id to it.quantity.subtract(it.receivedQuantity) }
                    .filterValues { it.signum() > 0 }
            } else {
                request.lines!!.associate { it.lineId to (it.quantity ?: throw BusinessRuleException("Quantity is required")) }
            }
        requested.keys.forEach { if (it !in byId) throw BusinessRuleException("Unknown purchase order line '$it'") }
        if (requested.isEmpty()) {
            throw BusinessRuleException("Nothing left to receive on this purchase order")
        }

        val updatedLines =
            po.lines.map { line ->
                val qty = requested[line.id] ?: return@map line
                if (qty.signum() <= 0) throw BusinessRuleException("Receive quantity must be positive")
                val remaining = line.quantity.subtract(line.receivedQuantity)
                if (qty > remaining) {
                    throw BusinessRuleException("Cannot receive $qty of '${line.productSku}'; only $remaining remaining")
                }
                stockMovementService.createMovement(
                    CreateStockMovementRequest(
                        type = StockMovementType.RECEIPT,
                        productId = line.productId,
                        warehouseId = po.warehouseId,
                        quantity = qty,
                        unitCost = line.unitCost,
                        reference = "PO-${po.poNumber}",
                    ),
                    organizationId,
                    userId,
                )
                line.apply { receivedQuantity = line.receivedQuantity.add(qty) }
            }

        val fullyReceived = updatedLines.all { it.receivedQuantity >= it.quantity }
        po.lines = updatedLines
        po.status = if (fullyReceived) PurchaseOrderStatus.RECEIVED else PurchaseOrderStatus.PARTIALLY_RECEIVED
        po.receivedAt = if (fullyReceived) LocalDateTime.now(ZoneOffset.UTC) else po.receivedAt
        return purchaseOrderRepository.save(po)
    }

    @Transactional
    fun generateBill(
        id: java.util.UUID,
        request: GenerateBillRequest?,
        organizationId: java.util.UUID,
        createdBy: java.util.UUID,
    ): Bill {
        val po = getPurchaseOrder(id, organizationId)
        if (po.status != PurchaseOrderStatus.PARTIALLY_RECEIVED && po.status != PurchaseOrderStatus.RECEIVED) {
            throw BusinessRuleException("Purchase order must have received goods before billing")
        }

        val byId = po.lines.associateBy { it.id }
        val requested =
            if (request?.lines.isNullOrEmpty()) {
                po.lines
                    .associate { it.id to (it.receivedQuantity.subtract(it.billedQuantity) to null as BigDecimal?) }
                    .filterValues { it.first.signum() > 0 }
            } else {
                request.lines!!.associate {
                    it.lineId to ((it.quantity ?: throw BusinessRuleException("Quantity is required")) to it.unitCost)
                }
            }
        requested.keys.forEach { if (it !in byId) throw BusinessRuleException("Unknown purchase order line '$it'") }
        if (requested.isEmpty()) {
            throw BusinessRuleException("Nothing left to bill on this purchase order")
        }

        val expenseAccountId = billExpenseAccountId(po, organizationId)
        val billLines = mutableListOf<BillLineRequest>()
        val updatedLines =
            po.lines.map { line ->
                val entry = requested[line.id] ?: return@map line
                val (qty, overrideCost) = entry
                if (qty.signum() <= 0) throw BusinessRuleException("Bill quantity must be positive")
                val billable = line.receivedQuantity.subtract(line.billedQuantity)
                if (qty > billable) {
                    throw BusinessRuleException(
                        "Cannot bill $qty of '${line.productSku}'; only $billable received and unbilled",
                    )
                }
                val unitCost = overrideCost ?: line.unitCost
                validatePriceWithinTolerance(line, unitCost)
                billLines.add(
                    BillLineRequest(
                        accountId = expenseAccountId,
                        amount = qty.multiply(unitCost),
                        description = "${line.productSku} - ${line.productName}",
                    ),
                )
                line.apply { billedQuantity = line.billedQuantity.add(qty) }
            }

        val today = LocalDate.now(ZoneOffset.UTC)
        val bill =
            billService.createBill(
                CreateBillRequest(
                    vendorId = po.vendorId,
                    date = today,
                    dueDate = today.plusDays(DEFAULT_BILL_TERM_DAYS),
                    referenceNumber = "PO-${po.poNumber}",
                    lines = billLines,
                ),
                organizationId,
                createdBy,
            )
        po.lines = updatedLines
        purchaseOrderRepository.save(po)
        return bill
    }

    /**
     * Three-way match preview: compares a vendor-supplied bill against the PO and
     * its receipts without creating anything. Flags over-billed quantities and
     * unit-cost variances beyond tolerance.
     */
    fun previewBillMatch(
        id: java.util.UUID,
        request: BillMatchRequest,
        organizationId: java.util.UUID,
    ): BillMatchResult {
        val po = getPurchaseOrder(id, organizationId)
        val byId = po.lines.associateBy { it.id }
        val results =
            request.lines.map { reqLine ->
                val line =
                    byId[reqLine.lineId]
                        ?: throw BusinessRuleException("Unknown purchase order line '${reqLine.lineId}'")
                val vendorQty = reqLine.quantity ?: throw BusinessRuleException("Quantity is required")
                val vendorCost = reqLine.unitCost ?: throw BusinessRuleException("Unit cost is required")
                val billable = line.receivedQuantity.subtract(line.billedQuantity)
                val status =
                    when {
                        vendorQty > billable -> MatchStatus.OVER_BILLED
                        priceVariance(line.unitCost, vendorCost) > PRICE_TOLERANCE -> MatchStatus.PRICE_VARIANCE
                        else -> MatchStatus.MATCHED
                    }
                BillMatchLineResult(
                    lineId = line.id,
                    productSku = line.productSku,
                    orderedQuantity = line.quantity,
                    receivedQuantity = line.receivedQuantity,
                    billedQuantity = line.billedQuantity,
                    billableQuantity = billable,
                    poUnitCost = line.unitCost,
                    vendorUnitCost = vendorCost,
                    vendorQuantity = vendorQty,
                    status = status,
                )
            }
        return BillMatchResult(
            purchaseOrderId = po.id,
            poNumber = po.poNumber,
            matched = results.all { it.status == MatchStatus.MATCHED },
            lines = results,
        )
    }

    private fun priceVariance(
        poCost: BigDecimal,
        vendorCost: BigDecimal,
    ): BigDecimal {
        if (poCost.signum() <= 0) return BigDecimal.ZERO
        return vendorCost.subtract(poCost).abs().divide(poCost, 6, RoundingMode.HALF_UP)
    }

    private fun billExpenseAccountId(
        po: PurchaseOrder,
        organizationId: java.util.UUID,
    ): java.util.UUID {
        val clearing = accountRepository.findByOrganizationIdAndCode(organizationId, INVENTORY_CLEARING_CODE)
        if (clearing.isPresent && clearing.get().isActive) {
            return clearing.get().id
        }
        val vendor = vendorService.getVendor(po.vendorId, organizationId)
        return vendor.defaultExpenseAccountId
            ?: throw BusinessRuleException(
                "No inventory clearing account ($INVENTORY_CLEARING_CODE) configured and vendor has no default expense account",
            )
    }

    private fun validatePriceWithinTolerance(
        line: PurchaseOrderLine,
        unitCost: BigDecimal,
    ) {
        if (line.unitCost.signum() <= 0) return
        if (priceVariance(line.unitCost, unitCost) > PRICE_TOLERANCE) {
            throw BusinessRuleException(
                "Billed unit cost $unitCost for '${line.productSku}' exceeds PO cost ${line.unitCost} beyond tolerance",
            )
        }
    }

    @Transactional
    fun closePurchaseOrder(
        id: java.util.UUID,
        organizationId: java.util.UUID,
    ): PurchaseOrder {
        val po = getPurchaseOrder(id, organizationId)
        if (po.status != PurchaseOrderStatus.RECEIVED) {
            throw BusinessRuleException("Only received purchase orders can be closed")
        }
        po.status = PurchaseOrderStatus.CLOSED
        return purchaseOrderRepository.save(po)
    }

    @Transactional
    fun cancelPurchaseOrder(
        id: java.util.UUID,
        organizationId: java.util.UUID,
        userId: java.util.UUID,
    ): PurchaseOrder {
        val po = getPurchaseOrder(id, organizationId)
        if (po.status == PurchaseOrderStatus.CANCELLED || po.status == PurchaseOrderStatus.CLOSED) {
            throw BusinessRuleException("Closed or cancelled purchase orders cannot be cancelled")
        }
        if (po.lines.any { it.billedQuantity.signum() > 0 }) {
            throw BusinessRuleException("Cannot cancel a purchase order that has generated bills; void the bills first")
        }

        val received = po.status == PurchaseOrderStatus.PARTIALLY_RECEIVED || po.status == PurchaseOrderStatus.RECEIVED
        if (received) {
            stockMovementService.reverseByReference("PO-${po.poNumber}", organizationId, userId)
        }

        po.lines = if (received) po.lines.map { it.apply { receivedQuantity = BigDecimal.ZERO } } else po.lines
        po.status = PurchaseOrderStatus.CANCELLED
        po.cancelledAt = LocalDateTime.now(ZoneOffset.UTC)
        return purchaseOrderRepository.save(po)
    }

    private fun saveWithRetry(
        organizationId: java.util.UUID,
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

    private companion object {
        const val INVENTORY_CLEARING_CODE = "2150"
        const val DEFAULT_BILL_TERM_DAYS = 30L
        val PRICE_TOLERANCE: BigDecimal = BigDecimal("0.05")
    }
}
