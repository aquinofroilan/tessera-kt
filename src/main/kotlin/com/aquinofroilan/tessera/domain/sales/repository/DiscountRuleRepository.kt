package com.aquinofroilan.tessera.domain.sales.repository

import com.aquinofroilan.tessera.domain.sales.model.DiscountRule
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface DiscountRuleRepository : JpaRepository<DiscountRule, UUID> {
    fun findByOrganizationId(organizationId: UUID): List<DiscountRule>

    fun findByIdAndOrganizationId(
        id: UUID,
        organizationId: UUID,
    ): Optional<DiscountRule>

    fun findByOrganizationIdAndIsActiveOrderByPriorityDesc(
        organizationId: UUID,
        isActive: Boolean,
    ): List<DiscountRule>

    fun existsByOrganizationIdAndCode(
        organizationId: UUID,
        code: String,
    ): Boolean
}
