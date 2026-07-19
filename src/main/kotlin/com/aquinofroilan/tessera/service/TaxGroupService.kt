package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateTaxGroupRequest
import com.aquinofroilan.tessera.dto.TaxSummaryResponse
import com.aquinofroilan.tessera.dto.UpdateTaxGroupRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.TaxGroup
import com.aquinofroilan.tessera.model.TaxRate
import com.aquinofroilan.tessera.repository.AccountRepository
import com.aquinofroilan.tessera.repository.JournalEntryRepository
import com.aquinofroilan.tessera.repository.TaxGroupRepository
import com.aquinofroilan.tessera.repository.TaxRateRepository
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@Service
class TaxGroupService(
    private val taxGroupRepository: TaxGroupRepository,
    private val taxRateRepository: TaxRateRepository,
    private val accountRepository: AccountRepository,
    private val journalEntryRepository: JournalEntryRepository,
) {
    @Transactional
    fun createTaxGroup(
        request: CreateTaxGroupRequest,
        organizationId: java.util.UUID,
    ): TaxGroup {
        if (request.taxRateIds.isEmpty()) {
            throw BusinessRuleException("At least one tax rate is required")
        }
        val uniqueRateIds = request.taxRateIds.distinct()
        val rates = validateAndLoadRates(uniqueRateIds, organizationId)
        val combinedRate = rates.fold(BigDecimal.ZERO) { sum, rate -> sum.add(rate.percentage) }

        val taxGroup =
            TaxGroup(
                name = request.name,
                code = request.code,
                taxRateIds = uniqueRateIds,
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
        taxGroupId: java.util.UUID,
        organizationId: java.util.UUID,
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
        taxGroupId: java.util.UUID,
        organizationId: java.util.UUID,
    ): Pair<TaxGroup, List<TaxRate>> {
        val taxGroup = getTaxGroup(taxGroupId, organizationId)
        val ratesById = taxRateRepository.findAllById(taxGroup.taxRateIds).associateBy { it.id }
        val missing = taxGroup.taxRateIds.filter { it !in ratesById }
        if (missing.isNotEmpty()) {
            throw BusinessRuleException("Tax rates not found: ${missing.joinToString(", ")}")
        }
        val crossOrg = ratesById.values.filter { it.organizationId != organizationId }.map { it.id }
        if (crossOrg.isNotEmpty()) {
            throw BusinessRuleException("Tax rates not found: ${crossOrg.joinToString(", ")}")
        }
        val orderedRates = taxGroup.taxRateIds.map { ratesById.getValue(it) }
        return taxGroup to orderedRates
    }

    fun listTaxGroups(
        organizationId: java.util.UUID,
        activeOnly: Boolean = false,
    ): List<TaxGroup> =
        if (activeOnly) {
            taxGroupRepository.findByOrganizationIdAndIsActive(organizationId, true)
        } else {
            taxGroupRepository.findByOrganizationId(organizationId)
        }

    @Transactional
    fun updateTaxGroup(
        taxGroupId: java.util.UUID,
        request: UpdateTaxGroupRequest,
        organizationId: java.util.UUID,
    ): TaxGroup {
        val taxGroup = getTaxGroup(taxGroupId, organizationId)

        if (!taxGroup.isActive) {
            throw BusinessRuleException("Cannot update inactive tax group")
        }

        if (request.name != null && request.name.isBlank()) {
            throw BusinessRuleException("Tax group name cannot be blank")
        }
        if (request.taxRateIds != null && request.taxRateIds.isEmpty()) {
            throw BusinessRuleException("At least one tax rate is required")
        }
        val newRateIds = request.taxRateIds?.distinct() ?: taxGroup.taxRateIds
        val rates = validateAndLoadRates(newRateIds, organizationId)

        val combinedRate = rates.fold(BigDecimal.ZERO) { sum, rate -> sum.add(rate.percentage) }

        taxGroup.apply {
            name = request.name ?: taxGroup.name
            taxRateIds = newRateIds
            this.combinedRate = combinedRate
        }

        return taxGroupRepository.save(taxGroup)
    }

    @Transactional
    fun deleteTaxGroup(
        taxGroupId: java.util.UUID,
        organizationId: java.util.UUID,
    ): TaxGroup {
        val taxGroup = getTaxGroup(taxGroupId, organizationId)

        if (!taxGroup.isActive) {
            throw BusinessRuleException("Tax group is already inactive")
        }

        taxGroup.isActive = false
        return taxGroupRepository.save(taxGroup)
    }

    fun calculateTaxAmount(
        taxGroupId: java.util.UUID?,
        organizationId: java.util.UUID,
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

    fun loadRatesByIds(ids: List<java.util.UUID>): List<TaxRate> = taxRateRepository.findAllById(ids)

    fun getTaxSummary(
        organizationId: java.util.UUID,
        startDate: LocalDate,
        endDate: LocalDate,
    ): TaxSummaryResponse {
        if (startDate.isAfter(endDate)) {
            throw BusinessRuleException("Start date must be on or before end date")
        }

        val payable = accountRepository.findByOrganizationIdAndCode(organizationId, "2300").orElse(null)
        val input = accountRepository.findByOrganizationIdAndCode(organizationId, "2310").orElse(null)
        val accountIds = listOfNotNull(payable?.id, input?.id)

        val totals =
            if (accountIds.isEmpty()) {
                emptyMap()
            } else {
                journalEntryRepository.aggregateAccountTotals(organizationId, accountIds, startDate, endDate)
            }

        val taxCollected =
            payable?.let { totals[it.id] }?.let {
                it.totalCredits.subtract(it.totalDebits)
            } ?: BigDecimal.ZERO
        val taxPaid =
            input?.let { totals[it.id] }?.let {
                it.totalDebits.subtract(it.totalCredits)
            } ?: BigDecimal.ZERO

        return TaxSummaryResponse(
            taxCollected = taxCollected,
            taxPaid = taxPaid,
            netTaxLiability = taxCollected.subtract(taxPaid),
            startDate = startDate.toString(),
            endDate = endDate.toString(),
        )
    }

    private fun validateAndLoadRates(
        taxRateIds: List<java.util.UUID>,
        organizationId: java.util.UUID,
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
