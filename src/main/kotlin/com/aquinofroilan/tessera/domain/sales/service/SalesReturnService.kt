package com.aquinofroilan.tessera.domain.sales.service

import com.aquinofroilan.tessera.domain.inventory.dto.CreateStockMovementRequest
import com.aquinofroilan.tessera.domain.inventory.model.StockMovementType
import com.aquinofroilan.tessera.domain.inventory.repository.ProductRepository
import com.aquinofroilan.tessera.domain.inventory.repository.WarehouseRepository
import com.aquinofroilan.tessera.domain.inventory.service.StockMovementService
import com.aquinofroilan.tessera.domain.sales.dto.CreateCreditNoteLineRequest
import com.aquinofroilan.tessera.domain.sales.dto.CreateCreditNoteRequest
import com.aquinofroilan.tessera.domain.sales.dto.CreateSalesReturnRequest
import com.aquinofroilan.tessera.domain.sales.dto.ReceiveSalesReturnRequest
import com.aquinofroilan.tessera.domain.sales.dto.SalesReturnResponse
import com.aquinofroilan.tessera.domain.sales.model.SalesReturn
import com.aquinofroilan.tessera.domain.sales.model.SalesReturnLine
import com.aquinofroilan.tessera.domain.sales.model.SalesReturnStatus
import com.aquinofroilan.tessera.domain.sales.repository.CustomerRepository
import com.aquinofroilan.tessera.domain.sales.repository.SalesReturnRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@Service
class SalesReturnService(
    private val salesReturnRepository: SalesReturnRepository,
    private val customerRepository: CustomerRepository,
    private val warehouseRepository: WarehouseRepository,
    private val productRepository: ProductRepository,
    private val stockMovementService: StockMovementService,
    private val creditNoteService: CreditNoteService,
) {
    @Transactional(readOnly = true)
    fun listSalesReturns(
        organizationId: UUID,
        customerId: UUID? = null,
        status: SalesReturnStatus? = null,
    ): List<SalesReturnResponse> {
        val returns =
            when {
                customerId != null -> salesReturnRepository.findByOrganizationIdAndCustomerId(organizationId, customerId)
                status != null -> salesReturnRepository.findByOrganizationIdAndStatus(organizationId, status)
                else -> salesReturnRepository.findByOrganizationId(organizationId)
            }
        return returns.map { SalesReturnResponse.from(it) }
    }

    @Transactional(readOnly = true)
    fun getSalesReturn(
        id: UUID,
        organizationId: UUID,
    ): SalesReturnResponse {
        val ret =
            salesReturnRepository.findByIdAndOrganizationId(id, organizationId).orElseThrow {
                ResourceNotFoundException("Sales return $id not found")
            }
        return SalesReturnResponse.from(ret)
    }

    @Transactional
    fun createSalesReturn(
        organizationId: UUID,
        userId: UUID,
        request: CreateSalesReturnRequest,
    ): SalesReturnResponse {
        val customer =
            customerRepository.findByIdAndOrganizationId(request.customerId, organizationId).orElseThrow {
                ResourceNotFoundException("Customer ${request.customerId} not found")
            }

        val warehouse =
            warehouseRepository.findById(request.warehouseId).orElseThrow {
                ResourceNotFoundException("Warehouse ${request.warehouseId} not found")
            }
        if (warehouse.organizationId != organizationId) {
            throw ResourceNotFoundException("Warehouse ${request.warehouseId} not found")
        }

        val count = salesReturnRepository.countByOrganizationId(organizationId)
        val returnNumber = "RMA-%05d".format(count + 1)

        var totalAmount = BigDecimal.ZERO
        val salesReturn =
            SalesReturn(
                organizationId = organizationId,
                returnNumber = returnNumber,
                customerId = customer.id,
                customerName = customer.name,
                salesOrderId = request.salesOrderId,
                invoiceId = request.invoiceId,
                warehouseId = warehouse.id,
                returnDate = request.returnDate ?: LocalDate.now(),
                status = SalesReturnStatus.REQUESTED,
                reason = request.reason,
                notes = request.notes?.trim(),
                restockInventory = request.restockInventory ?: true,
                totalAmount = BigDecimal.ZERO,
                createdBy = userId,
            )

        request.lines.forEachIndexed { index, lineReq ->
            val product =
                productRepository.findById(lineReq.productId).orElseThrow {
                    ResourceNotFoundException("Product ${lineReq.productId} not found")
                }
            val lineTotal = lineReq.unitPrice.multiply(lineReq.quantity).setScale(2, java.math.RoundingMode.HALF_UP)
            totalAmount = totalAmount.add(lineTotal)

            salesReturn.lines.add(
                SalesReturnLine(
                    salesReturnId = salesReturn.id,
                    lineNumber = index + 1,
                    productId = product.id,
                    productSku = product.sku,
                    productName = product.name,
                    quantity = lineReq.quantity,
                    unitPrice = lineReq.unitPrice,
                    lineTotal = lineTotal,
                    conditionNotes = lineReq.conditionNotes?.trim(),
                ),
            )
        }

        salesReturn.totalAmount = totalAmount.setScale(2, java.math.RoundingMode.HALF_UP)
        val saved = salesReturnRepository.save(salesReturn)
        return SalesReturnResponse.from(saved)
    }

    @Transactional
    fun approveSalesReturn(
        id: UUID,
        organizationId: UUID,
        userId: UUID,
    ): SalesReturnResponse {
        val ret =
            salesReturnRepository.findByIdAndOrganizationId(id, organizationId).orElseThrow {
                ResourceNotFoundException("Sales return $id not found")
            }

        if (ret.status != SalesReturnStatus.REQUESTED) {
            throw BusinessRuleException("Only REQUESTED sales returns can be approved (current: ${ret.status})")
        }

        ret.status = SalesReturnStatus.APPROVED
        ret.approvedBy = userId
        ret.approvedAt = LocalDateTime.now(ZoneOffset.UTC)

        val saved = salesReturnRepository.save(ret)
        return SalesReturnResponse.from(saved)
    }

    @Transactional
    fun receiveSalesReturn(
        id: UUID,
        organizationId: UUID,
        userId: UUID,
        request: ReceiveSalesReturnRequest? = null,
    ): SalesReturnResponse {
        val ret =
            salesReturnRepository.findByIdAndOrganizationId(id, organizationId).orElseThrow {
                ResourceNotFoundException("Sales return $id not found")
            }

        if (ret.status != SalesReturnStatus.APPROVED) {
            throw BusinessRuleException("Only APPROVED sales returns can be received (current: ${ret.status})")
        }

        ret.status = SalesReturnStatus.RECEIVED
        ret.receivedBy = userId
        ret.receivedAt = LocalDateTime.now(ZoneOffset.UTC)

        ret.lines.forEach { line ->
            line.receivedQuantity = line.quantity
            if (request?.conditionNotes != null) {
                line.conditionNotes = request.conditionNotes
            }

            if (ret.restockInventory) {
                stockMovementService.createMovement(
                    CreateStockMovementRequest(
                        type = StockMovementType.RECEIPT,
                        productId = line.productId,
                        warehouseId = ret.warehouseId,
                        quantity = line.quantity,
                        unitCost = line.unitPrice,
                        reference = ret.returnNumber,
                        notes = "Restock from Sales Return ${ret.returnNumber}",
                    ),
                    organizationId,
                    userId,
                )
            }
        }

        val saved = salesReturnRepository.save(ret)
        return SalesReturnResponse.from(saved)
    }

    @Transactional
    fun completeSalesReturn(
        id: UUID,
        organizationId: UUID,
        userId: UUID,
        issueCreditNote: Boolean = true,
    ): SalesReturnResponse {
        val ret =
            salesReturnRepository.findByIdAndOrganizationId(id, organizationId).orElseThrow {
                ResourceNotFoundException("Sales return $id not found")
            }

        if (ret.status != SalesReturnStatus.RECEIVED) {
            throw BusinessRuleException("Only RECEIVED sales returns can be completed (current: ${ret.status})")
        }

        ret.status = SalesReturnStatus.COMPLETED

        if (issueCreditNote) {
            val cnLines =
                ret.lines.map { line ->
                    CreateCreditNoteLineRequest(
                        productId = line.productId,
                        description = "Return of ${line.productName} (${line.productSku})",
                        quantity = line.quantity,
                        unitPrice = line.unitPrice,
                    )
                }

            val cnReq =
                CreateCreditNoteRequest(
                    customerId = ret.customerId,
                    salesReturnId = ret.id,
                    invoiceId = ret.invoiceId,
                    reason = "Credit Note for Sales Return ${ret.returnNumber}",
                    lines = cnLines,
                )

            val cn = creditNoteService.createCreditNote(organizationId, userId, cnReq)
            creditNoteService.approveCreditNote(cn.id, organizationId, userId)
        }

        val saved = salesReturnRepository.save(ret)
        return SalesReturnResponse.from(saved)
    }

    @Transactional
    fun cancelSalesReturn(
        id: UUID,
        organizationId: UUID,
    ): SalesReturnResponse {
        val ret =
            salesReturnRepository.findByIdAndOrganizationId(id, organizationId).orElseThrow {
                ResourceNotFoundException("Sales return $id not found")
            }

        if (ret.status != SalesReturnStatus.REQUESTED && ret.status != SalesReturnStatus.APPROVED) {
            throw BusinessRuleException("Cannot cancel sales return in status ${ret.status}")
        }

        ret.status = SalesReturnStatus.CANCELLED
        val saved = salesReturnRepository.save(ret)
        return SalesReturnResponse.from(saved)
    }
}
