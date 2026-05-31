package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.InventoryWaSnapshot
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface InventoryWaSnapshotRepository : JpaRepository<InventoryWaSnapshot, String> {
    fun findByOrganizationIdAndProductIdAndWarehouseId(
        organizationId: String,
        productId: String,
        warehouseId: String,
    ): Optional<InventoryWaSnapshot>

    fun findByOrganizationId(organizationId: String): List<InventoryWaSnapshot>
}
