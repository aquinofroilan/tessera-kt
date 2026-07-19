package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.InventoryReorderRule
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface InventoryReorderRuleRepository : JpaRepository<InventoryReorderRule, java.util.UUID> {
    fun findByOrganizationId(organizationId: java.util.UUID): List<InventoryReorderRule>

    fun findByOrganizationIdAndProductIdAndWarehouseId(
        organizationId: java.util.UUID,
        productId: java.util.UUID,
        warehouseId: java.util.UUID,
    ): Optional<InventoryReorderRule>
}
