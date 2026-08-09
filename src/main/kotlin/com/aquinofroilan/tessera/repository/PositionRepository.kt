package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.Position
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface PositionRepository : JpaRepository<Position, java.util.UUID> {
    fun findByOrganizationId(organizationId: java.util.UUID): List<Position>

    fun findByOrganizationIdAndIsActive(
        organizationId: java.util.UUID,
        isActive: Boolean,
    ): List<Position>

    fun findByOrganizationIdAndCode(
        organizationId: java.util.UUID,
        code: String,
    ): Optional<Position>
}
