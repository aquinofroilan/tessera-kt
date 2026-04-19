package com.froilan.synectix.service

import com.froilan.synectix.dto.CreateTaxRateRequest
import com.froilan.synectix.dto.UpdateTaxRateRequest
import com.froilan.synectix.exception.BusinessRuleException
import com.froilan.synectix.exception.ResourceNotFoundException
import com.froilan.synectix.model.TaxRate
import com.froilan.synectix.repository.TaxGroupRepository
import com.froilan.synectix.repository.TaxRateRepository
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
        organizationId: String,
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
        taxRateId: String,
        organizationId: String,
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
        organizationId: String,
        activeOnly: Boolean = false,
    ): List<TaxRate> =
        if (activeOnly) {
            taxRateRepository.findByOrganizationIdAndIsActive(organizationId, true)
        } else {
            taxRateRepository.findByOrganizationId(organizationId)
        }

    @Transactional
    fun updateTaxRate(
        taxRateId: String,
        request: UpdateTaxRateRequest,
        organizationId: String,
    ): TaxRate {
        val taxRate = getTaxRate(taxRateId, organizationId)

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

        val updated =
            taxRate.copy(
                name = request.name ?: taxRate.name,
                percentage = request.percentage ?: taxRate.percentage,
                authority = request.authority ?: taxRate.authority,
            )

        val saved = taxRateRepository.save(updated)

        if (request.percentage != null && request.percentage.compareTo(taxRate.percentage) != 0) {
            cascadeCombinedRate(taxRateId, organizationId)
        }

        return saved
    }

    @Transactional
    fun deleteTaxRate(
        taxRateId: String,
        organizationId: String,
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

        return taxRateRepository.save(taxRate.copy(isActive = false))
    }

    private fun cascadeCombinedRate(
        taxRateId: String,
        organizationId: String,
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
            taxGroupRepository.save(group.copy(combinedRate = newCombinedRate))
        }
    }
}
