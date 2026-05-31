package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.Position
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface PositionRepository : JpaRepository<Position, String> {
    fun findByOrganizationId(organizationId: String): List<Position>

    fun findByOrganizationIdAndIsActive(
        organizationId: String,
        isActive: Boolean,
    ): List<Position>

    fun findByOrganizationIdAndCode(
        organizationId: String,
        code: String,
    ): Optional<Position>
}
