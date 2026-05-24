package com.froilan.synectix.repository

import com.froilan.synectix.model.InventoryCostLayer
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface InventoryCostLayerRepository : MongoRepository<InventoryCostLayer, String> {
    fun findByOrganizationIdAndProductIdAndWarehouseIdOrderByOccurredAtAsc(
        organizationId: String,
        productId: String,
        warehouseId: String,
    ): List<InventoryCostLayer>

    fun findByOrganizationId(organizationId: String): List<InventoryCostLayer>
}
