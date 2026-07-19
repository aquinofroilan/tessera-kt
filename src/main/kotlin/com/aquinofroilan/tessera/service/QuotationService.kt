package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.ConvertQuotationRequest
import com.aquinofroilan.tessera.dto.CreateQuotationRequest
import com.aquinofroilan.tessera.dto.CreateSalesOrderLineRequest
import com.aquinofroilan.tessera.dto.CreateSalesOrderRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.Quotation
import com.aquinofroilan.tessera.model.QuotationLine
import com.aquinofroilan.tessera.model.QuotationStatus
import com.aquinofroilan.tessera.model.SalesOrder
import com.aquinofroilan.tessera.repository.QuotationRepository
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class QuotationService(
    private val quotationRepository: QuotationRepository,
    private val customerService: CustomerService,
    private val warehouseService: WarehouseService,
    private val productService: ProductService,
    private val salesOrderService: SalesOrderService,
) {
    @Transactional
    fun createQuotation(
        request: CreateQuotationRequest,
        organizationId: java.util.UUID,
        createdBy: java.util.UUID,
    ): Quotation {
        val customer = customerService.getCustomer(request.customerId, organizationId)
        if (!customer.isActive) {
            throw BusinessRuleException("Cannot create a quotation for an inactive customer")
        }
        request.warehouseId?.let {
            val warehouse = warehouseService.getWarehouse(it, organizationId)
            if (!warehouse.isActive) {
                throw BusinessRuleException("Cannot create a quotation for an inactive warehouse")
            }
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
                QuotationLine(
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
            Quotation(
                quoteNumber = number,
                customerId = customer.id,
                customerName = customer.name,
                warehouseId = request.warehouseId,
                quoteDate = request.quoteDate ?: throw BusinessRuleException("Quote date is required"),
                validUntil = request.validUntil,
                referenceNumber = request.referenceNumber,
                organizationId = organizationId,
                lines = lines,
                totalAmount = total,
                createdBy = createdBy,
            )
        }
    }

    fun getQuotation(
        id: java.util.UUID,
        organizationId: java.util.UUID,
    ): Quotation {
        val quote =
            quotationRepository.findById(id).orElseThrow {
                ResourceNotFoundException("Quotation not found")
            }
        if (quote.organizationId != organizationId) {
            throw ResourceNotFoundException("Quotation not found")
        }
        return quote
    }

    fun listQuotations(
        organizationId: java.util.UUID,
        status: QuotationStatus? = null,
        customerId: java.util.UUID? = null,
    ): List<Quotation> =
        when {
            status != null && customerId != null ->
                quotationRepository.findByOrganizationIdAndStatusAndCustomerId(organizationId, status, customerId)
            status != null -> quotationRepository.findByOrganizationIdAndStatus(organizationId, status)
            customerId != null -> quotationRepository.findByOrganizationIdAndCustomerId(organizationId, customerId)
            else -> quotationRepository.findByOrganizationId(organizationId)
        }

    @Transactional
    fun sendQuotation(
        id: java.util.UUID,
        organizationId: java.util.UUID,
    ): Quotation {
        val quote = getQuotation(id, organizationId)
        if (quote.status != QuotationStatus.DRAFT) {
            throw BusinessRuleException("Only draft quotations can be sent")
        }
        return quotationRepository.save(
            quote.copy(status = QuotationStatus.SENT, sentAt = LocalDateTime.now(ZoneOffset.UTC)),
        )
    }

    @Transactional
    fun acceptQuotation(
        id: java.util.UUID,
        organizationId: java.util.UUID,
    ): Quotation {
        val quote = getQuotation(id, organizationId)
        if (quote.status != QuotationStatus.SENT) {
            throw BusinessRuleException("Only sent quotations can be accepted")
        }
        requireNotExpired(quote)
        return quotationRepository.save(
            quote.copy(status = QuotationStatus.ACCEPTED, decidedAt = LocalDateTime.now(ZoneOffset.UTC)),
        )
    }

    @Transactional
    fun rejectQuotation(
        id: java.util.UUID,
        reason: String?,
        organizationId: java.util.UUID,
    ): Quotation {
        val quote = getQuotation(id, organizationId)
        if (quote.status != QuotationStatus.SENT) {
            throw BusinessRuleException("Only sent quotations can be rejected")
        }
        return quotationRepository.save(
            quote.copy(
                status = QuotationStatus.REJECTED,
                decisionReason = reason,
                decidedAt = LocalDateTime.now(ZoneOffset.UTC),
            ),
        )
    }

    @Transactional
    fun cancelQuotation(
        id: java.util.UUID,
        organizationId: java.util.UUID,
    ): Quotation {
        val quote = getQuotation(id, organizationId)
        if (quote.status == QuotationStatus.CONVERTED) {
            throw BusinessRuleException("A converted quotation cannot be cancelled")
        }
        if (quote.status == QuotationStatus.CANCELLED || quote.status == QuotationStatus.REJECTED) {
            throw BusinessRuleException("Quotation is already ${quote.status.name.lowercase()}")
        }
        return quotationRepository.save(quote.copy(status = QuotationStatus.CANCELLED))
    }

    /**
     * Converts an accepted quote into a sales order. The warehouse and order
     * date may be supplied on the request or fall back to the quote's warehouse
     * / today. Marks the quote CONVERTED and links it to the new sales order.
     */
    @Transactional
    fun convertToSalesOrder(
        id: java.util.UUID,
        request: ConvertQuotationRequest,
        organizationId: java.util.UUID,
        createdBy: java.util.UUID,
    ): SalesOrder {
        val quote = getQuotation(id, organizationId)
        if (quote.status != QuotationStatus.ACCEPTED) {
            throw BusinessRuleException("Only accepted quotations can be converted")
        }
        val warehouseId =
            request.warehouseId ?: quote.warehouseId
                ?: throw BusinessRuleException("A warehouse is required to convert this quotation")

        val soLines =
            quote.lines.map { line ->
                CreateSalesOrderLineRequest(
                    productId = line.productId,
                    quantity = line.quantity,
                    unitPrice = line.unitPrice,
                    description = line.description,
                )
            }

        val salesOrder =
            salesOrderService.createSalesOrder(
                CreateSalesOrderRequest(
                    customerId = quote.customerId,
                    warehouseId = warehouseId,
                    orderDate = request.orderDate ?: LocalDate.now(ZoneOffset.UTC),
                    expectedDate = request.expectedDate,
                    referenceNumber = quote.quoteNumber,
                    lines = soLines,
                ),
                organizationId,
                createdBy,
            )

        quotationRepository.save(
            quote.copy(
                status = QuotationStatus.CONVERTED,
                convertedSalesOrderId = salesOrder.id,
            ),
        )
        return salesOrder
    }

    private fun requireNotExpired(quote: Quotation) {
        val validUntil = quote.validUntil ?: return
        if (validUntil.isBefore(LocalDate.now(ZoneOffset.UTC))) {
            throw BusinessRuleException("Quotation expired on $validUntil")
        }
    }

    private fun saveWithRetry(
        organizationId: java.util.UUID,
        maxRetries: Int = 3,
        build: (String) -> Quotation,
    ): Quotation {
        repeat(maxRetries) { attempt ->
            val count = quotationRepository.countByOrganizationId(organizationId)
            val number = "QT-${(count + 1).toString().padStart(4, '0')}"
            try {
                return quotationRepository.save(build(number))
            } catch (e: DuplicateKeyException) {
                if (attempt == maxRetries - 1) {
                    throw IllegalStateException("Failed to generate unique quotation number: $number", e)
                }
            }
        }
        throw IllegalStateException("Failed to generate unique quotation number")
    }
}
