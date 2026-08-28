package com.aquinofroilan.tessera.domain.sales.service

import com.aquinofroilan.tessera.domain.inventory.repository.ProductRepository
import com.aquinofroilan.tessera.domain.sales.dto.BatchSetPriceListLinesRequest
import com.aquinofroilan.tessera.domain.sales.dto.CreatePriceListLineRequest
import com.aquinofroilan.tessera.domain.sales.dto.CreatePriceListRequest
import com.aquinofroilan.tessera.domain.sales.dto.PriceListLineDto
import com.aquinofroilan.tessera.domain.sales.dto.PriceListResponse
import com.aquinofroilan.tessera.domain.sales.dto.UpdatePriceListRequest
import com.aquinofroilan.tessera.domain.sales.model.CustomerSegment
import com.aquinofroilan.tessera.domain.sales.model.PriceList
import com.aquinofroilan.tessera.domain.sales.model.PriceListLine
import com.aquinofroilan.tessera.domain.sales.repository.PriceListLineRepository
import com.aquinofroilan.tessera.domain.sales.repository.PriceListRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Locale
import java.util.UUID

@Service
class PriceListService(
    private val priceListRepository: PriceListRepository,
    private val priceListLineRepository: PriceListLineRepository,
    private val productRepository: ProductRepository,
) {
    @Transactional(readOnly = true)
    fun listPriceLists(
        organizationId: UUID,
        currency: String? = null,
        customerSegment: CustomerSegment? = null,
        isActive: Boolean? = null,
    ): List<PriceListResponse> {
        val lists = priceListRepository.findByOrganizationId(organizationId)
        return lists
            .filter { pl ->
                val matchesCurr = currency?.let { pl.currency.equals(it, ignoreCase = true) } ?: true
                val matchesSeg = customerSegment?.let { pl.customerSegment == it } ?: true
                val matchesActive = isActive?.let { pl.isActive == it } ?: true
                matchesCurr && matchesSeg && matchesActive
            }.map { PriceListResponse.from(it) }
    }

    @Transactional(readOnly = true)
    fun getPriceList(
        id: UUID,
        organizationId: UUID,
    ): PriceListResponse {
        val priceList =
            priceListRepository.findByIdAndOrganizationId(id, organizationId).orElseThrow {
                ResourceNotFoundException("Price list $id not found")
            }
        return PriceListResponse.from(priceList)
    }

    @Transactional
    fun createPriceList(
        organizationId: UUID,
        request: CreatePriceListRequest,
    ): PriceListResponse {
        val trimmedCode = request.code.trim().uppercase(Locale.ROOT)
        if (priceListRepository.existsByOrganizationIdAndCode(organizationId, trimmedCode)) {
            throw BusinessRuleException("Price list with code '$trimmedCode' already exists")
        }

        val currency = request.currency.trim().uppercase(Locale.ROOT)

        val priceList =
            PriceList(
                organizationId = organizationId,
                name = request.name.trim(),
                code = trimmedCode,
                currency = currency,
                customerSegment = request.customerSegment,
                isDefault = request.isDefault ?: false,
                isActive = true,
                validFrom = request.validFrom,
                validTo = request.validTo,
                description = request.description?.trim(),
            )

        if (request.isDefault == true) {
            unsetExistingDefaults(organizationId, currency, request.customerSegment)
        }

        request.lines?.forEach { lineReq ->
            val product =
                productRepository.findById(lineReq.productId).orElseThrow {
                    ResourceNotFoundException("Product ${lineReq.productId} not found")
                }
            priceList.lines.add(
                PriceListLine(
                    priceListId = priceList.id,
                    productId = product.id,
                    productSku = product.sku,
                    unitPrice = lineReq.unitPrice,
                    minQuantity = lineReq.minQuantity ?: BigDecimal.ONE,
                ),
            )
        }

        val saved = priceListRepository.save(priceList)
        return PriceListResponse.from(saved)
    }

    @Transactional
    fun updatePriceList(
        id: UUID,
        organizationId: UUID,
        request: UpdatePriceListRequest,
    ): PriceListResponse {
        val priceList =
            priceListRepository.findByIdAndOrganizationId(id, organizationId).orElseThrow {
                ResourceNotFoundException("Price list $id not found")
            }

        request.name?.let { priceList.name = it.trim() }
        request.customerSegment?.let { priceList.customerSegment = it }
        request.description?.let { priceList.description = it.trim() }
        request.validFrom?.let { priceList.validFrom = it }
        request.validTo?.let { priceList.validTo = it }
        request.isActive?.let { priceList.isActive = it }

        request.isDefault?.let { isDef ->
            if (isDef && !priceList.isDefault) {
                unsetExistingDefaults(organizationId, priceList.currency, priceList.customerSegment)
            }
            priceList.isDefault = isDef
        }

        val saved = priceListRepository.save(priceList)
        return PriceListResponse.from(saved)
    }

    @Transactional
    fun addOrUpdateLine(
        priceListId: UUID,
        organizationId: UUID,
        request: CreatePriceListLineRequest,
    ): PriceListLineDto {
        val priceList =
            priceListRepository.findByIdAndOrganizationId(priceListId, organizationId).orElseThrow {
                ResourceNotFoundException("Price list $priceListId not found")
            }

        val product =
            productRepository.findById(request.productId).orElseThrow {
                ResourceNotFoundException("Product ${request.productId} not found")
            }

        val targetMinQty = request.minQuantity ?: BigDecimal.ONE
        val existingOpt =
            priceListLineRepository.findByPriceListIdAndProductIdAndMinQuantity(
                priceListId,
                product.id,
                targetMinQty,
            )

        val line =
            if (existingOpt.isPresent) {
                val existing = existingOpt.get()
                existing.unitPrice = request.unitPrice
                existing.updatedAt = LocalDateTime.now(ZoneOffset.UTC)
                priceListLineRepository.save(existing)
            } else {
                val newLine =
                    PriceListLine(
                        priceListId = priceList.id,
                        productId = product.id,
                        productSku = product.sku,
                        unitPrice = request.unitPrice,
                        minQuantity = targetMinQty,
                    )
                priceList.lines.add(newLine)
                priceListRepository.save(priceList)
                newLine
            }

        return PriceListLineDto.from(line)
    }

    @Transactional
    fun deleteLine(
        priceListId: UUID,
        organizationId: UUID,
        lineId: UUID,
    ) {
        priceListRepository.findByIdAndOrganizationId(priceListId, organizationId).orElseThrow {
            ResourceNotFoundException("Price list $priceListId not found")
        }
        priceListLineRepository.deleteByPriceListIdAndId(priceListId, lineId)
    }

    @Transactional
    fun batchSetLines(
        priceListId: UUID,
        organizationId: UUID,
        request: BatchSetPriceListLinesRequest,
    ): PriceListResponse {
        val priceList =
            priceListRepository.findByIdAndOrganizationId(priceListId, organizationId).orElseThrow {
                ResourceNotFoundException("Price list $priceListId not found")
            }

        priceList.lines.clear()

        request.lines.forEach { lineReq ->
            val product =
                productRepository.findById(lineReq.productId).orElseThrow {
                    ResourceNotFoundException("Product ${lineReq.productId} not found")
                }
            priceList.lines.add(
                PriceListLine(
                    priceListId = priceList.id,
                    productId = product.id,
                    productSku = product.sku,
                    unitPrice = lineReq.unitPrice,
                    minQuantity = lineReq.minQuantity ?: BigDecimal.ONE,
                ),
            )
        }

        val saved = priceListRepository.save(priceList)
        return PriceListResponse.from(saved)
    }

    @Transactional
    fun deletePriceList(
        id: UUID,
        organizationId: UUID,
    ) {
        val priceList =
            priceListRepository.findByIdAndOrganizationId(id, organizationId).orElseThrow {
                ResourceNotFoundException("Price list $id not found")
            }
        priceListRepository.delete(priceList)
    }

    private fun unsetExistingDefaults(
        organizationId: UUID,
        currency: String,
        segment: CustomerSegment?,
    ) {
        val existing =
            priceListRepository.findByOrganizationIdAndCurrencyAndCustomerSegmentAndIsActive(
                organizationId,
                currency,
                segment,
                true,
            )
        existing.forEach {
            if (it.isDefault) {
                it.isDefault = false
                priceListRepository.save(it)
            }
        }
    }
}
