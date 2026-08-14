package com.aquinofroilan.tessera.domain.inventory.repository

import com.aquinofroilan.tessera.domain.inventory.model.InventoryCountSession
import com.aquinofroilan.tessera.domain.inventory.model.InventoryCountStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

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
