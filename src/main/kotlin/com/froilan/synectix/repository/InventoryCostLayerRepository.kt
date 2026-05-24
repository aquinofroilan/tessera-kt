package com.froilan.synectix.repository

import com.froilan.synectix.model.InventoryCostLayer
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface InventoryCostLayerRepository : JpaRepository<InventoryCostLayer, String> {
    fun findByOrganizationIdAndProductIdAndWarehouseIdOrderByOccurredAtAsc(
        organizationId: String,
        productId: String,
        warehouseId: String,
    ): List<InventoryCostLayer>

    fun findByOrganizationId(organizationId: String): List<InventoryCostLayer>
}
