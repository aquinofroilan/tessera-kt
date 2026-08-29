package com.aquinofroilan.tessera.domain.mfg.repository

import com.aquinofroilan.tessera.domain.mfg.model.EngineeringChangeOrder
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface EngineeringChangeOrderRepository : JpaRepository<EngineeringChangeOrder, UUID> {
    fun findByOrganizationId(organizationId: UUID): List<EngineeringChangeOrder>

    fun findByOrganizationIdAndEcoNumber(
        organizationId: UUID,
        ecoNumber: String,
    ): EngineeringChangeOrder?
}
