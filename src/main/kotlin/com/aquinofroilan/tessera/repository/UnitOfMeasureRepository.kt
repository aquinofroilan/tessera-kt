package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.UnitOfMeasure
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface UnitOfMeasureRepository : JpaRepository<UnitOfMeasure, String> {
    fun findByOrganizationId(organizationId: String): List<UnitOfMeasure>

    fun findByOrganizationIdAndIsActive(
        organizationId: String,
        isActive: Boolean,
    ): List<UnitOfMeasure>

    fun findByOrganizationIdAndCode(
        organizationId: String,
        code: String,
    ): Optional<UnitOfMeasure>
}
