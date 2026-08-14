package com.aquinofroilan.tessera.domain.finance.service

import com.aquinofroilan.tessera.domain.finance.dto.CreateTaxRateRequest
import com.aquinofroilan.tessera.domain.finance.dto.UpdateTaxRateRequest
import com.aquinofroilan.tessera.domain.finance.model.TaxRate
import com.aquinofroilan.tessera.domain.finance.repository.TaxGroupRepository
import com.aquinofroilan.tessera.domain.finance.repository.TaxRateRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class TaxRateService(
    private val taxRateRepository: TaxRateRepository,
    private val taxGroupRepository: TaxGroupRepository,
) {
    @Transactional
    fun createTaxRate(
        request: CreateTaxRateRequest,
        organizationId: java.util.UUID,
    ): TaxRate {
        val taxRate =
            TaxRate(
                name = request.name,
                code = request.code,
                percentage = request.percentage,
                authority = request.authority,
                organizationId = organizationId,
            )

        return try {
            taxRateRepository.save(taxRate)
        } catch (e: DuplicateKeyException) {
            throw BusinessRuleException(
                "Tax rate code '${request.code}' already exists in this organization",
                e,
            )
        }
    }

    fun getTaxRate(
        taxRateId: java.util.UUID,
        organizationId: java.util.UUID,
    ): TaxRate {
        val taxRate =
            taxRateRepository.findById(taxRateId).orElseThrow {
                ResourceNotFoundException("Tax rate not found")
            }
        if (taxRate.organizationId != organizationId) {
            throw ResourceNotFoundException("Tax rate not found")
        }
        return taxRate
    }

    fun listTaxRates(
        organizationId: java.util.UUID,
        activeOnly: Boolean = false,
    ): List<TaxRate> =
        if (activeOnly) {
            taxRateRepository.findByOrganizationIdAndIsActive(organizationId, true)
        } else {
            taxRateRepository.findByOrganizationId(organizationId)
        }

    @Transactional
    fun updateTaxRate(
        taxRateId: java.util.UUID,
        request: UpdateTaxRateRequest,
        organizationId: java.util.UUID,
    ): TaxRate {
        val taxRate = getTaxRate(taxRateId, organizationId)
        val previousPercentage = taxRate.percentage

        if (!taxRate.isActive) {
            throw BusinessRuleException("Cannot update inactive tax rate")
        }

        if (request.name != null && request.name.isBlank()) {
            throw BusinessRuleException("Tax rate name cannot be blank")
        }
        if (request.authority != null && request.authority.isBlank()) {
            throw BusinessRuleException("Tax authority cannot be blank")
        }
        if (request.percentage != null && request.percentage <= BigDecimal.ZERO) {
            throw BusinessRuleException("Percentage must be positive")
        }

        taxRate.apply {
            name = request.name ?: taxRate.name
            percentage = request.percentage ?: taxRate.percentage
            authority = request.authority ?: taxRate.authority
        }

        val saved = taxRateRepository.save(taxRate)

        if (request.percentage != null && request.percentage.compareTo(previousPercentage) != 0) {
            cascadeCombinedRate(taxRateId, organizationId)
        }

        return saved
    }

    @Transactional
    fun deleteTaxRate(
        taxRateId: java.util.UUID,
        organizationId: java.util.UUID,
    ): TaxRate {
        val taxRate = getTaxRate(taxRateId, organizationId)

        if (!taxRate.isActive) {
            throw BusinessRuleException("Tax rate is already inactive")
        }

        val activeGroups =
            taxGroupRepository
                .findByOrganizationIdAndTaxRateIdsContaining(organizationId, taxRateId)
                .filter { it.isActive }
        if (activeGroups.isNotEmpty()) {
            throw BusinessRuleException("Cannot deactivate tax rate used in active tax groups")
        }

        taxRate.isActive = false
        return taxRateRepository.save(taxRate)
    }

    private fun cascadeCombinedRate(
        taxRateId: java.util.UUID,
        organizationId: java.util.UUID,
    ) {
        val groups = taxGroupRepository.findByOrganizationIdAndTaxRateIdsContaining(organizationId, taxRateId)
        if (groups.isEmpty()) return

        val allRateIds = groups.flatMap { it.taxRateIds }.distinct()
        val ratesById = taxRateRepository.findAllById(allRateIds).associateBy { it.id }

        val missing = allRateIds.filter { it !in ratesById }
        if (missing.isNotEmpty()) {
            throw BusinessRuleException("Tax rates not found: ${missing.joinToString(", ")}")
        }
        val crossOrg = ratesById.values.filter { it.organizationId != organizationId }.map { it.id }
        if (crossOrg.isNotEmpty()) {
            throw BusinessRuleException("Tax rates not found: ${crossOrg.joinToString(", ")}")
        }

        groups.forEach { group ->
            val newCombinedRate =
                group.taxRateIds.fold(BigDecimal.ZERO) { sum, id ->
                    sum.add(ratesById.getValue(id).percentage)
                }
            group.combinedRate = newCombinedRate
            taxGroupRepository.save(group)
        }
    }
}
