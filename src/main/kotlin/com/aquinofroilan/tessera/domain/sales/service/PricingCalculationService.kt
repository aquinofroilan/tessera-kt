package com.aquinofroilan.tessera.domain.sales.service

import com.aquinofroilan.tessera.domain.inventory.repository.ProductRepository
import com.aquinofroilan.tessera.domain.sales.dto.CalculatePriceRequest
import com.aquinofroilan.tessera.domain.sales.dto.CalculatePriceResponse
import com.aquinofroilan.tessera.domain.sales.model.Customer
import com.aquinofroilan.tessera.domain.sales.model.CustomerSegment
import com.aquinofroilan.tessera.domain.sales.model.DiscountRule
import com.aquinofroilan.tessera.domain.sales.model.DiscountType
import com.aquinofroilan.tessera.domain.sales.model.PriceList
import com.aquinofroilan.tessera.domain.sales.repository.CustomerRepository
import com.aquinofroilan.tessera.domain.sales.repository.DiscountRuleRepository
import com.aquinofroilan.tessera.domain.sales.repository.PriceListLineRepository
import com.aquinofroilan.tessera.domain.sales.repository.PriceListRepository
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.Locale
import java.util.UUID

@Service
class PricingCalculationService(
    private val productRepository: ProductRepository,
    private val customerRepository: CustomerRepository,
    private val priceListRepository: PriceListRepository,
    private val priceListLineRepository: PriceListLineRepository,
    private val discountRuleRepository: DiscountRuleRepository,
) {
    @Transactional(readOnly = true)
    fun calculatePrice(
        organizationId: UUID,
        request: CalculatePriceRequest,
    ): CalculatePriceResponse {
        val product =
            productRepository.findById(request.productId).orElseThrow {
                ResourceNotFoundException("Product ${request.productId} not found")
            }

        val orderDate = request.orderDate ?: LocalDate.now()
        val currency = (request.currency ?: "USD").trim().uppercase(Locale.ROOT)
        val quantity = request.quantity ?: BigDecimal.ONE

        var customer: Customer? = null
        var segment: CustomerSegment = CustomerSegment.RETAIL

        if (request.customerId != null) {
            customer =
                customerRepository.findByIdAndOrganizationId(request.customerId, organizationId).orElse(null)
            if (customer != null) {
                segment = customer.customerSegment
            }
        }

        // 1. Resolve Price List
        val priceList = resolvePriceList(organizationId, customer, segment, currency, orderDate)

        // 2. Resolve Base Unit Price
        var baseUnitPrice = product.listPrice
        if (priceList != null) {
            val matchingLines =
                priceListLineRepository.findByPriceListIdAndProductIdAndMinQuantityLessThanEqualOrderByMinQuantityDesc(
                    priceList.id,
                    product.id,
                    quantity,
                )
            if (matchingLines.isNotEmpty()) {
                baseUnitPrice = matchingLines.first().unitPrice
            }
        }

        // 3. Resolve Discount Rule
        val matchingDiscountRule =
            resolveDiscountRule(
                organizationId = organizationId,
                customerId = request.customerId,
                segment = segment,
                productId = product.id,
                priceListId = priceList?.id,
                quantity = quantity,
                baseOrderAmount = baseUnitPrice.multiply(quantity),
                orderDate = orderDate,
            )

        var discountType: DiscountType? = null
        var discountValue: BigDecimal? = null
        var discountAmountPerUnit = BigDecimal.ZERO

        if (matchingDiscountRule != null) {
            discountType = matchingDiscountRule.discountType
            discountValue = matchingDiscountRule.discountValue

            discountAmountPerUnit =
                when (matchingDiscountRule.discountType) {
                    DiscountType.PERCENTAGE, DiscountType.VOLUME_TIER -> {
                        baseUnitPrice
                            .multiply(matchingDiscountRule.discountValue)
                            .divide(BigDecimal("100"), 4, RoundingMode.HALF_UP)
                    }
                    DiscountType.FIXED_AMOUNT -> {
                        matchingDiscountRule.discountValue.min(baseUnitPrice)
                    }
                }
        }

        val effectiveUnitPrice =
            (baseUnitPrice.subtract(discountAmountPerUnit)).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP)
        val totalAmount = effectiveUnitPrice.multiply(quantity).setScale(2, RoundingMode.HALF_UP)

        return CalculatePriceResponse(
            productId = product.id,
            productSku = product.sku,
            productName = product.name,
            currency = currency,
            quantity = quantity,
            priceListId = priceList?.id,
            priceListName = priceList?.name,
            baseCatalogPrice = product.listPrice,
            baseUnitPrice = baseUnitPrice.setScale(4, RoundingMode.HALF_UP),
            discountType = discountType,
            discountValue = discountValue,
            discountAmountPerUnit = discountAmountPerUnit.setScale(4, RoundingMode.HALF_UP),
            effectiveUnitPrice = effectiveUnitPrice,
            totalAmount = totalAmount,
            appliedDiscountRuleCode = matchingDiscountRule?.code,
            appliedDiscountRuleName = matchingDiscountRule?.name,
        )
    }

    private fun resolvePriceList(
        organizationId: UUID,
        customer: Customer?,
        segment: CustomerSegment,
        currency: String,
        orderDate: LocalDate,
    ): PriceList? {
        // Customer specific default price list
        if (customer?.defaultPriceListId != null) {
            val customPl =
                priceListRepository.findByIdAndOrganizationId(customer.defaultPriceListId!!, organizationId).orElse(null)
            if (customPl != null && customPl.isActive && isDateValid(customPl.validFrom, customPl.validTo, orderDate)) {
                return customPl
            }
        }

        // Segment price list
        val segmentLists =
            priceListRepository.findByOrganizationIdAndCurrencyAndCustomerSegmentAndIsActive(
                organizationId,
                currency,
                segment,
                true,
            )
        val validSegmentPl = segmentLists.firstOrNull { isDateValid(it.validFrom, it.validTo, orderDate) }
        if (validSegmentPl != null) {
            return validSegmentPl
        }

        // Default price list for currency
        val defaultPl =
            priceListRepository
                .findByOrganizationIdAndCurrencyAndIsDefaultAndIsActive(organizationId, currency, true, true)
                .orElse(null)
        if (defaultPl != null && isDateValid(defaultPl.validFrom, defaultPl.validTo, orderDate)) {
            return defaultPl
        }

        return null
    }

    private fun resolveDiscountRule(
        organizationId: UUID,
        customerId: UUID?,
        segment: CustomerSegment,
        productId: UUID,
        priceListId: UUID?,
        quantity: BigDecimal,
        baseOrderAmount: BigDecimal,
        orderDate: LocalDate,
    ): DiscountRule? {
        val rules =
            discountRuleRepository.findByOrganizationIdAndIsActiveOrderByPriorityDesc(
                organizationId,
                true,
            )

        return rules.firstOrNull { rule ->
            val dateMatches = isDateValid(rule.validFrom, rule.validTo, orderDate)
            val customerMatches = rule.customerId == null || rule.customerId == customerId
            val segmentMatches = rule.customerSegment == null || rule.customerSegment == segment
            val productMatches = rule.productId == null || rule.productId == productId
            val priceListMatches = rule.priceListId == null || rule.priceListId == priceListId
            val quantityMatches = rule.minQuantity == null || quantity >= rule.minQuantity!!
            val amountMatches = rule.minOrderAmount == null || baseOrderAmount >= rule.minOrderAmount!!

            dateMatches &&
                customerMatches &&
                segmentMatches &&
                productMatches &&
                priceListMatches &&
                quantityMatches &&
                amountMatches
        }
    }

    private fun isDateValid(
        validFrom: LocalDate?,
        validTo: LocalDate?,
        date: LocalDate,
    ): Boolean {
        if (validFrom != null && date.isBefore(validFrom)) return false
        if (validTo != null && date.isAfter(validTo)) return false
        return true
    }
}
