package com.froilan.synectix.repository

import com.froilan.synectix.model.InventoryWaSnapshot
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface InventoryWaSnapshotRepository : MongoRepository<InventoryWaSnapshot, String> {
    fun findByOrganizationIdAndProductIdAndWarehouseId(
        organizationId: String,
        productId: String,
        warehouseId: String,
    ): Optional<InventoryWaSnapshot>

    fun findByOrganizationId(organizationId: String): List<InventoryWaSnapshot>
}
