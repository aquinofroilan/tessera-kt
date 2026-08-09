package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.InventoryCountSession
import com.aquinofroilan.tessera.model.InventoryCountStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface InventoryCountSessionRepository : JpaRepository<InventoryCountSession, String> {
    fun findByOrganizationId(organizationId: String): List<InventoryCountSession>

    fun findByOrganizationIdAndStatus(
        organizationId: String,
        status: InventoryCountStatus,
    ): List<InventoryCountSession>

    fun findByOrganizationIdAndCode(
        organizationId: String,
        code: String,
    ): Optional<InventoryCountSession>
}
