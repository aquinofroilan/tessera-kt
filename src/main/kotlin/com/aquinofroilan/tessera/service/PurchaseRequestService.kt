package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.ConvertPurchaseRequestRequest
import com.aquinofroilan.tessera.dto.CreatePurchaseOrderLineRequest
import com.aquinofroilan.tessera.dto.CreatePurchaseOrderRequest
import com.aquinofroilan.tessera.dto.CreatePurchaseRequestRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.PurchaseOrder
import com.aquinofroilan.tessera.model.PurchaseRequest
import com.aquinofroilan.tessera.model.PurchaseRequestLine
import com.aquinofroilan.tessera.model.PurchaseRequestStatus
import com.aquinofroilan.tessera.repository.PurchaseRequestRepository
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class PurchaseRequestService(
    private val purchaseRequestRepository: PurchaseRequestRepository,
    private val productService: ProductService,
    private val vendorService: VendorService,
    private val warehouseService: WarehouseService,
    private val purchaseOrderService: PurchaseOrderService,
) {
    @Transactional
    fun createPurchaseRequest(
        request: CreatePurchaseRequestRequest,
        organizationId: java.util.UUID,
        requestedBy: java.util.UUID,
    ): PurchaseRequest {
        request.suggestedVendorId?.let { vendorService.getVendor(it, organizationId) }
        request.warehouseId?.let { warehouseService.getWarehouse(it, organizationId) }

        val lines =
            request.lines.mapIndexed { index, lineReq ->
                val quantity = lineReq.quantity ?: throw BusinessRuleException("Quantity is required")
                if (quantity.signum() <= 0) {
                    throw BusinessRuleException("Line quantity must be positive")
                }
                if (lineReq.estimatedUnitCost != null && lineReq.estimatedUnitCost.signum() < 0) {
                    throw BusinessRuleException("Estimated unit cost must not be negative")
                }
                val product = productService.getProduct(lineReq.productId, organizationId)
                if (!product.isActive) {
                    throw BusinessRuleException("Product '${product.sku}' is inactive")
                }
                PurchaseRequestLine(
                    lineNumber = index + 1,
                    productId = product.id,
                    productSku = product.sku,
                    productName = product.name,
                    quantity = quantity,
                    estimatedUnitCost = lineReq.estimatedUnitCost,
                    description = lineReq.description,
                )
            }

        return saveWithRetry(organizationId) { number ->
            PurchaseRequest(
                prNumber = number,
                organizationId = organizationId,
                suggestedVendorId = request.suggestedVendorId,
                warehouseId = request.warehouseId,
                justification = request.justification,
                lines = lines,
                requestedBy = requestedBy,
            )
        }
    }

    fun getPurchaseRequest(
        id: java.util.UUID,
        organizationId: java.util.UUID,
    ): PurchaseRequest {
        val pr =
            purchaseRequestRepository.findById(id).orElseThrow {
                ResourceNotFoundException("Purchase request not found")
            }
        if (pr.organizationId != organizationId) {
            throw ResourceNotFoundException("Purchase request not found")
        }
        return pr
    }

    fun listPurchaseRequests(
        organizationId: java.util.UUID,
        status: PurchaseRequestStatus? = null,
        requestedBy: java.util.UUID? = null,
    ): List<PurchaseRequest> =
        when {
            status != null -> purchaseRequestRepository.findByOrganizationIdAndStatus(organizationId, status)
            requestedBy != null -> purchaseRequestRepository.findByOrganizationIdAndRequestedBy(organizationId, requestedBy)
            else -> purchaseRequestRepository.findByOrganizationId(organizationId)
        }

    @Transactional
    fun submitPurchaseRequest(
        id: java.util.UUID,
        organizationId: java.util.UUID,
    ): PurchaseRequest {
        val pr = getPurchaseRequest(id, organizationId)
        if (pr.status != PurchaseRequestStatus.DRAFT) {
            throw BusinessRuleException("Only draft purchase requests can be submitted")
        }
        pr.status = PurchaseRequestStatus.SUBMITTED
        return purchaseRequestRepository.save(pr)
    }

    @Transactional
    fun approvePurchaseRequest(
        id: java.util.UUID,
        organizationId: java.util.UUID,
        decidedBy: java.util.UUID,
    ): PurchaseRequest {
        val pr = getPurchaseRequest(id, organizationId)
        if (pr.status != PurchaseRequestStatus.SUBMITTED) {
            throw BusinessRuleException("Only submitted purchase requests can be approved")
        }
        pr.status = PurchaseRequestStatus.APPROVED
        pr.decidedBy = decidedBy
        pr.decidedAt = LocalDateTime.now(ZoneOffset.UTC)
        return purchaseRequestRepository.save(pr)
    }

    @Transactional
    fun rejectPurchaseRequest(
        id: java.util.UUID,
        reason: String?,
        organizationId: java.util.UUID,
        decidedBy: java.util.UUID,
    ): PurchaseRequest {
        val pr = getPurchaseRequest(id, organizationId)
        if (pr.status != PurchaseRequestStatus.SUBMITTED) {
            throw BusinessRuleException("Only submitted purchase requests can be rejected")
        }
        pr.status = PurchaseRequestStatus.REJECTED
        pr.decisionReason = reason
        pr.decidedBy = decidedBy
        pr.decidedAt = LocalDateTime.now(ZoneOffset.UTC)
        return purchaseRequestRepository.save(pr)
    }

    @Transactional
    fun cancelPurchaseRequest(
        id: java.util.UUID,
        organizationId: java.util.UUID,
    ): PurchaseRequest {
        val pr = getPurchaseRequest(id, organizationId)
        if (pr.status == PurchaseRequestStatus.CONVERTED) {
            throw BusinessRuleException("A converted purchase request cannot be cancelled")
        }
        if (pr.status == PurchaseRequestStatus.CANCELLED || pr.status == PurchaseRequestStatus.REJECTED) {
            throw BusinessRuleException("Purchase request is already ${pr.status.name.lowercase()}")
        }
        pr.status = PurchaseRequestStatus.CANCELLED
        return purchaseRequestRepository.save(pr)
    }

    /**
     * Converts an approved request into a purchase order. The vendor, warehouse
     * and order date may be supplied on the request or fall back to the values
     * captured on the requisition. Each line needs a unit cost — taken from a
     * per-line override or the line's estimated cost. Marks the request CONVERTED
     * and links it to the new PO.
     */
    @Transactional
    fun convertToPurchaseOrder(
        id: java.util.UUID,
        request: ConvertPurchaseRequestRequest,
        organizationId: java.util.UUID,
        createdBy: java.util.UUID,
    ): PurchaseOrder {
        val pr = getPurchaseRequest(id, organizationId)
        if (pr.status != PurchaseRequestStatus.APPROVED) {
            throw BusinessRuleException("Only approved purchase requests can be converted")
        }
        val vendorId =
            request.vendorId ?: pr.suggestedVendorId
                ?: throw BusinessRuleException("A vendor is required to convert this request")
        val warehouseId =
            request.warehouseId ?: pr.warehouseId
                ?: throw BusinessRuleException("A warehouse is required to convert this request")
        val costOverrides = request.lineCosts.orEmpty().associate { it.lineId to it.unitCost }

        val poLines =
            pr.lines.map { line ->
                val unitCost =
                    costOverrides[line.id] ?: line.estimatedUnitCost
                        ?: throw BusinessRuleException("Line '${line.productSku}' has no unit cost; provide one to convert")
                CreatePurchaseOrderLineRequest(
                    productId = line.productId,
                    quantity = line.quantity,
                    unitCost = unitCost,
                    description = line.description,
                )
            }

        val po =
            purchaseOrderService.createPurchaseOrder(
                CreatePurchaseOrderRequest(
                    vendorId = vendorId,
                    warehouseId = warehouseId,
                    orderDate = request.orderDate ?: LocalDate.now(ZoneOffset.UTC),
                    expectedDate = request.expectedDate,
                    referenceNumber = pr.prNumber,
                    lines = poLines,
                ),
                organizationId,
                createdBy,
            )

        pr.status = PurchaseRequestStatus.CONVERTED
        pr.convertedPurchaseOrderId = po.id
        purchaseRequestRepository.save(pr)
        return po
    }

    private fun saveWithRetry(
        organizationId: java.util.UUID,
        maxRetries: Int = 3,
        build: (String) -> PurchaseRequest,
    ): PurchaseRequest {
        repeat(maxRetries) { attempt ->
            val count = purchaseRequestRepository.countByOrganizationId(organizationId)
            val number = "PR-${(count + 1).toString().padStart(4, '0')}"
            try {
                return purchaseRequestRepository.save(build(number))
            } catch (e: DuplicateKeyException) {
                if (attempt == maxRetries - 1) {
                    throw IllegalStateException("Failed to generate unique PR number: $number", e)
                }
            }
        }
        throw IllegalStateException("Failed to generate unique PR number")
    }
}
