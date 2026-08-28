package com.aquinofroilan.tessera.domain.inventory.repository

import com.aquinofroilan.tessera.domain.inventory.model.UnitOfMeasure
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface UnitOfMeasureRepository : JpaRepository<UnitOfMeasure, UUID> {
    fun findByOrganizationId(organizationId: UUID): List<UnitOfMeasure>

    fun findByOrganizationIdAndIsActive(
        organizationId: UUID,
        isActive: Boolean,
    ): List<UnitOfMeasure>

    fun findByOrganizationIdAndCode(
        organizationId: UUID,
        code: String,
    ): Optional<UnitOfMeasure>
}
