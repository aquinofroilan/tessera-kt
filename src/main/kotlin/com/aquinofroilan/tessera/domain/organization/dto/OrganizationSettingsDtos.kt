package com.aquinofroilan.tessera.domain.organization.dto

import com.aquinofroilan.tessera.domain.organization.model.InventoryCostingMethod
import com.aquinofroilan.tessera.domain.organization.model.Organizations
import jakarta.validation.constraints.Size
import java.time.LocalDateTime
import java.util.UUID

data class OrganizationSettingsResponse(
    val id: UUID,
    val orgSlug: String,
    val name: String,
    val description: String?,
    val legalName: String,
    val tradeName: String,
    val baseCurrency: String,
    val fiscalYearStart: LocalDateTime,
    val timezone: String,
    val logoUrl: String?,
    val status: String,
    val inventoryCostingMethod: InventoryCostingMethod,
    val inventoryGlPostingEnabled: Boolean,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(org: Organizations): OrganizationSettingsResponse =
            OrganizationSettingsResponse(
                id = org.uuid,
                orgSlug = org.orgSlug,
                name = org.name,
                description = org.description,
                legalName = org.legalName,
                tradeName = org.tradeName,
                baseCurrency = org.baseCurrency,
                fiscalYearStart = org.fiscalYearStart,
                timezone = org.timezone,
                logoUrl = org.logoUrl,
                status = org.status,
                inventoryCostingMethod = org.inventoryCostingMethod,
                inventoryGlPostingEnabled = org.inventoryGlPostingEnabled,
                createdAt = org.createdAt,
            )
    }
}

data class UpdateOrganizationSettingsRequest(
    val name: String? = null,
    val description: String? = null,
    val legalName: String? = null,
    val tradeName: String? = null,
    @field:Size(min = 3, max = 3, message = "Base currency must be a 3-letter ISO code")
    val baseCurrency: String? = null,
    val fiscalYearStart: LocalDateTime? = null,
    val timezone: String? = null,
    val logoUrl: String? = null,
    val inventoryCostingMethod: InventoryCostingMethod? = null,
    val inventoryGlPostingEnabled: Boolean? = null,
)
