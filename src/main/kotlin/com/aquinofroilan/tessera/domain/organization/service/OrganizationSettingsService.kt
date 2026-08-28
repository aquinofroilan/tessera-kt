package com.aquinofroilan.tessera.domain.organization.service

import com.aquinofroilan.tessera.domain.finance.repository.CurrencyRepository
import com.aquinofroilan.tessera.domain.organization.dto.OrganizationSettingsResponse
import com.aquinofroilan.tessera.domain.organization.dto.UpdateOrganizationSettingsRequest
import com.aquinofroilan.tessera.domain.organization.repository.OrganizationRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZoneId
import java.util.Locale
import java.util.UUID

@Service
class OrganizationSettingsService(
    private val organizationRepository: OrganizationRepository,
    private val currencyRepository: CurrencyRepository,
) {
    @Transactional(readOnly = true)
    fun getSettings(organizationId: UUID): OrganizationSettingsResponse {
        val org =
            organizationRepository.findById(organizationId).orElseThrow {
                ResourceNotFoundException("Organization $organizationId not found")
            }
        return OrganizationSettingsResponse.from(org)
    }

    @Transactional
    fun updateSettings(
        organizationId: UUID,
        request: UpdateOrganizationSettingsRequest,
    ): OrganizationSettingsResponse {
        val org =
            organizationRepository.findById(organizationId).orElseThrow {
                ResourceNotFoundException("Organization $organizationId not found")
            }

        request.name?.let {
            val trimmedName = it.trim()
            if (trimmedName.isEmpty()) {
                throw BusinessRuleException("Organization name cannot be blank")
            }
            if (trimmedName != org.name && organizationRepository.existsByName(trimmedName)) {
                throw BusinessRuleException("Organization with name '$trimmedName' already exists")
            }
            org.name = trimmedName
        }

        request.description?.let {
            org.description = it.trim().ifEmpty { null }
        }

        request.legalName?.let {
            val trimmedLegal = it.trim()
            if (trimmedLegal.isEmpty()) {
                throw BusinessRuleException("Legal name cannot be blank")
            }
            org.legalName = trimmedLegal
        }

        request.tradeName?.let {
            val trimmedTrade = it.trim()
            if (trimmedTrade.isEmpty()) {
                throw BusinessRuleException("Trade name cannot be blank")
            }
            org.tradeName = trimmedTrade
        }

        request.baseCurrency?.let {
            val currencyCode = it.trim().uppercase(Locale.ROOT)
            if (currencyCode.length != 3) {
                throw BusinessRuleException("Base currency must be a 3-letter ISO code")
            }
            if (!currencyRepository.existsById(currencyCode)) {
                throw BusinessRuleException("Invalid or unsupported currency: $currencyCode")
            }
            org.baseCurrency = currencyCode
        }

        request.fiscalYearStart?.let {
            org.fiscalYearStart = it
        }

        request.timezone?.let {
            val tzStr = it.trim()
            if (tzStr.isEmpty()) {
                throw BusinessRuleException("Timezone cannot be blank")
            }
            try {
                ZoneId.of(tzStr)
            } catch (e: Exception) {
                throw BusinessRuleException("Invalid timezone: '$tzStr'")
            }
            org.timezone = tzStr
        }

        request.logoUrl?.let {
            org.logoUrl = it.trim().ifEmpty { null }
        }

        request.inventoryCostingMethod?.let {
            org.inventoryCostingMethod = it
        }

        request.inventoryGlPostingEnabled?.let {
            org.inventoryGlPostingEnabled = it
        }

        val saved = organizationRepository.save(org)
        return OrganizationSettingsResponse.from(saved)
    }
}
