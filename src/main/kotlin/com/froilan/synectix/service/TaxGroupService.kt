package com.froilan.synectix.service

import com.froilan.synectix.dto.CreateTaxGroupRequest
import com.froilan.synectix.dto.UpdateTaxGroupRequest
import com.froilan.synectix.exception.BusinessRuleException
import com.froilan.synectix.exception.ResourceNotFoundException
import com.froilan.synectix.model.TaxGroup
import com.froilan.synectix.model.TaxRate
import com.froilan.synectix.repository.TaxGroupRepository
import com.froilan.synectix.repository.TaxRateRepository
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class TaxGroupService(
    private val taxGroupRepository: TaxGroupRepository,
    private val taxRateRepository: TaxRateRepository,
) {
    @Transactional
    fun createTaxGroup(
        request: CreateTaxGroupRequest,
        organizationId: String,
    ): TaxGroup {
        val rates = validateAndLoadRates(request.taxRateIds, organizationId)
        val combinedRate = rates.fold(BigDecimal.ZERO) { sum, rate -> sum.add(rate.percentage) }

        val taxGroup =
            TaxGroup(
                name = request.name,
                code = request.code,
                taxRateIds = request.taxRateIds,
                combinedRate = combinedRate,
                organizationId = organizationId,
            )

        return try {
            taxGroupRepository.save(taxGroup)
        } catch (e: DuplicateKeyException) {
            throw BusinessRuleException(
                "Tax group code '${request.code}' already exists in this organization",
                e,
            )
        }
    }

    fun getTaxGroup(
        taxGroupId: String,
        organizationId: String,
    ): TaxGroup {
        val taxGroup =
            taxGroupRepository.findById(taxGroupId).orElseThrow {
                ResourceNotFoundException("Tax group not found")
            }
        if (taxGroup.organizationId != organizationId) {
            throw ResourceNotFoundException("Tax group not found")
        }
        return taxGroup
    }

    fun getTaxGroupWithRates(
        taxGroupId: String,
        organizationId: String,
    ): Pair<TaxGroup, List<TaxRate>> {
        val taxGroup = getTaxGroup(taxGroupId, organizationId)
        val rates = taxRateRepository.findAllById(taxGroup.taxRateIds)
        return taxGroup to rates
    }

    fun listTaxGroups(
        organizationId: String,
        activeOnly: Boolean = false,
    ): List<TaxGroup> =
        if (activeOnly) {
            taxGroupRepository.findByOrganizationIdAndIsActive(organizationId, true)
        } else {
            taxGroupRepository.findByOrganizationId(organizationId)
        }

    @Transactional
    fun updateTaxGroup(
        taxGroupId: String,
        request: UpdateTaxGroupRequest,
        organizationId: String,
    ): TaxGroup {
        val taxGroup = getTaxGroup(taxGroupId, organizationId)

        if (!taxGroup.isActive) {
            throw BusinessRuleException("Cannot update inactive tax group")
        }

        val newRateIds = request.taxRateIds ?: taxGroup.taxRateIds
        val rates =
            if (request.taxRateIds != null) {
                validateAndLoadRates(newRateIds, organizationId)
            } else {
                taxRateRepository.findAllById(taxGroup.taxRateIds)
            }

        val combinedRate = rates.fold(BigDecimal.ZERO) { sum, rate -> sum.add(rate.percentage) }

        val updated =
            taxGroup.copy(
                name = request.name ?: taxGroup.name,
                taxRateIds = newRateIds,
                combinedRate = combinedRate,
            )

        return taxGroupRepository.save(updated)
    }

    @Transactional
    fun deleteTaxGroup(
        taxGroupId: String,
        organizationId: String,
    ): TaxGroup {
        val taxGroup = getTaxGroup(taxGroupId, organizationId)

        if (!taxGroup.isActive) {
            throw BusinessRuleException("Tax group is already inactive")
        }

        return taxGroupRepository.save(taxGroup.copy(isActive = false))
    }

    fun calculateTaxAmount(
        taxGroupId: String?,
        organizationId: String,
        baseAmount: BigDecimal,
    ): BigDecimal {
        if (taxGroupId == null) return BigDecimal.ZERO

        val taxGroup = getTaxGroup(taxGroupId, organizationId)
        if (!taxGroup.isActive) {
            throw BusinessRuleException("Tax group '${taxGroup.code}' is inactive")
        }

        return baseAmount
            .multiply(taxGroup.combinedRate)
            .divide(BigDecimal("100"), 2, RoundingMode.HALF_UP)
    }

    private fun validateAndLoadRates(
        taxRateIds: List<String>,
        organizationId: String,
    ): List<TaxRate> {
        val rates = taxRateRepository.findAllById(taxRateIds)
        val foundIds = rates.map { it.id }.toSet()

        val missing = taxRateIds.filter { it !in foundIds }
        if (missing.isNotEmpty()) {
            throw BusinessRuleException("Tax rates not found: ${missing.joinToString(", ")}")
        }

        rates.forEach { rate ->
            if (rate.organizationId != organizationId) {
                throw BusinessRuleException("Tax rate '${rate.id}' not found")
            }
            if (!rate.isActive) {
                throw BusinessRuleException("Tax rate '${rate.code}' is inactive")
            }
        }

        return rates
    }
}
