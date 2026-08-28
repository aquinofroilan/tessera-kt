package com.aquinofroilan.tessera.domain.sales.repository

import com.aquinofroilan.tessera.domain.sales.model.CustomerSegment
import com.aquinofroilan.tessera.domain.sales.model.PriceList
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface PriceListRepository : JpaRepository<PriceList, UUID> {
    fun findByOrganizationId(organizationId: UUID): List<PriceList>

    fun findByIdAndOrganizationId(
        id: UUID,
        organizationId: UUID,
    ): Optional<PriceList>

    fun findByOrganizationIdAndCode(
        organizationId: UUID,
        code: String,
    ): Optional<PriceList>

    fun findByOrganizationIdAndCurrencyAndCustomerSegmentAndIsActive(
        organizationId: UUID,
        currency: String,
        customerSegment: CustomerSegment?,
        isActive: Boolean,
    ): List<PriceList>

    fun findByOrganizationIdAndCurrencyAndIsDefaultAndIsActive(
        organizationId: UUID,
        currency: String,
        isDefault: Boolean,
        isActive: Boolean,
    ): Optional<PriceList>

    fun findByOrganizationIdAndIsActive(
        organizationId: UUID,
        isActive: Boolean,
    ): List<PriceList>

    fun existsByOrganizationIdAndCode(
        organizationId: UUID,
        code: String,
    ): Boolean
}
