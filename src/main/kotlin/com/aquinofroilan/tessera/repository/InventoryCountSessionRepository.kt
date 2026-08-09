package com.aquinofroilan.tessera.repository

import java.util.UUID

import com.aquinofroilan.tessera.model.InventoryCountSession
import com.aquinofroilan.tessera.model.InventoryCountStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface InventoryCountSessionRepository : JpaRepository<InventoryCountSession, UUID> {
    fun findByOrganizationId(organizationId: UUID): List<InventoryCountSession>

    fun findByOrganizationIdAndStatus(
        organizationId: UUID,
        status: InventoryCountStatus,
    ): List<InventoryCountSession>

    fun findByOrganizationIdAndCode(
        organizationId: UUID,
        code: String,
    ): Optional<InventoryCountSession>
}
