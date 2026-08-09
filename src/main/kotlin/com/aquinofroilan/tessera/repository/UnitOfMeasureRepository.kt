package com.aquinofroilan.tessera.repository

import java.util.UUID

import com.aquinofroilan.tessera.model.UnitOfMeasure
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

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
