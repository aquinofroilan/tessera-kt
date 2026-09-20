package com.aquinofroilan.tessera.domain.inventory.repository

import com.aquinofroilan.tessera.domain.inventory.model.InventoryHold
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface InventoryHoldRepository : JpaRepository<InventoryHold, UUID> {
    fun findByOrganizationId(
        organizationId: UUID,
        pageable: Pageable,
    ): Page<InventoryHold>

    fun findByOrganizationIdAndProductId(
        organizationId: UUID,
        productId: UUID,
        pageable: Pageable,
    ): Page<InventoryHold>
}
