package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.InventoryCostLayer
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface InventoryCostLayerRepository : JpaRepository<InventoryCostLayer, java.util.UUID> {
    fun findByOrganizationIdAndProductIdAndWarehouseIdOrderByOccurredAtAsc(
        organizationId: java.util.UUID,
        productId: java.util.UUID,
        warehouseId: java.util.UUID,
    ): List<InventoryCostLayer>

    fun findByOrganizationId(organizationId: java.util.UUID): List<InventoryCostLayer>
}
