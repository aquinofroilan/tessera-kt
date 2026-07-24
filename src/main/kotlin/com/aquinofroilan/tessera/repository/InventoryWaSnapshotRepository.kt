package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.InventoryWaSnapshot
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface InventoryWaSnapshotRepository : JpaRepository<InventoryWaSnapshot, java.util.UUID> {
    fun findByOrganizationIdAndProductIdAndWarehouseId(
        organizationId: java.util.UUID,
        productId: java.util.UUID,
        warehouseId: java.util.UUID,
    ): Optional<InventoryWaSnapshot>

    fun findByOrganizationId(organizationId: java.util.UUID): List<InventoryWaSnapshot>
}
