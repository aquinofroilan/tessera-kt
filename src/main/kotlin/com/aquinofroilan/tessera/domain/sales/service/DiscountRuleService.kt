package com.aquinofroilan.tessera.domain.sales.service

import com.aquinofroilan.tessera.domain.sales.dto.CreateDiscountRuleRequest
import com.aquinofroilan.tessera.domain.sales.dto.DiscountRuleResponse
import com.aquinofroilan.tessera.domain.sales.dto.UpdateDiscountRuleRequest
import com.aquinofroilan.tessera.domain.sales.model.DiscountRule
import com.aquinofroilan.tessera.domain.sales.repository.DiscountRuleRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Locale
import java.util.UUID

@Service
class DiscountRuleService(
    private val discountRuleRepository: DiscountRuleRepository,
) {
    @Transactional(readOnly = true)
    fun listDiscountRules(organizationId: UUID): List<DiscountRuleResponse> =
        discountRuleRepository
            .findByOrganizationId(organizationId)
            .sortedByDescending { it.priority }
            .map { DiscountRuleResponse.from(it) }

    @Transactional(readOnly = true)
    fun getDiscountRule(
        id: UUID,
        organizationId: UUID,
    ): DiscountRuleResponse {
        val rule =
            discountRuleRepository.findByIdAndOrganizationId(id, organizationId).orElseThrow {
                ResourceNotFoundException("Discount rule $id not found")
            }
        return DiscountRuleResponse.from(rule)
    }

    @Transactional
    fun createDiscountRule(
        organizationId: UUID,
        request: CreateDiscountRuleRequest,
    ): DiscountRuleResponse {
        val trimmedCode = request.code.trim().uppercase(Locale.ROOT)
        if (discountRuleRepository.existsByOrganizationIdAndCode(organizationId, trimmedCode)) {
            throw BusinessRuleException("Discount rule with code '$trimmedCode' already exists")
        }

        val rule =
            DiscountRule(
                organizationId = organizationId,
                name = request.name.trim(),
                code = trimmedCode,
                discountType = request.discountType,
                discountValue = request.discountValue,
                customerSegment = request.customerSegment,
                customerId = request.customerId,
                productId = request.productId,
                priceListId = request.priceListId,
                minQuantity = request.minQuantity,
                minOrderAmount = request.minOrderAmount,
                validFrom = request.validFrom,
                validTo = request.validTo,
                priority = request.priority ?: 0,
                description = request.description?.trim(),
            )

        val saved = discountRuleRepository.save(rule)
        return DiscountRuleResponse.from(saved)
    }

    @Transactional
    fun updateDiscountRule(
        id: UUID,
        organizationId: UUID,
        request: UpdateDiscountRuleRequest,
    ): DiscountRuleResponse {
        val rule =
            discountRuleRepository.findByIdAndOrganizationId(id, organizationId).orElseThrow {
                ResourceNotFoundException("Discount rule $id not found")
            }

        request.name?.let { rule.name = it.trim() }
        request.discountType?.let { rule.discountType = it }
        request.discountValue?.let { rule.discountValue = it }
        request.customerSegment?.let { rule.customerSegment = it }
        request.customerId?.let { rule.customerId = it }
        request.productId?.let { rule.productId = it }
        request.priceListId?.let { rule.priceListId = it }
        request.minQuantity?.let { rule.minQuantity = it }
        request.minOrderAmount?.let { rule.minOrderAmount = it }
        request.validFrom?.let { rule.validFrom = it }
        request.validTo?.let { rule.validTo = it }
        request.isActive?.let { rule.isActive = it }
        request.priority?.let { rule.priority = it }
        request.description?.let { rule.description = it.trim() }

        val saved = discountRuleRepository.save(rule)
        return DiscountRuleResponse.from(saved)
    }

    @Transactional
    fun deleteDiscountRule(
        id: UUID,
        organizationId: UUID,
    ) {
        val rule =
            discountRuleRepository.findByIdAndOrganizationId(id, organizationId).orElseThrow {
                ResourceNotFoundException("Discount rule $id not found")
            }
        discountRuleRepository.delete(rule)
    }
}
