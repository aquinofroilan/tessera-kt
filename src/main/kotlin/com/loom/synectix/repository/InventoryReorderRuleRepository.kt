package com.loom.synectix.repository

import com.loom.synectix.model.InventoryReorderRule
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface InventoryReorderRuleRepository : JpaRepository<InventoryReorderRule, String> {
    fun findByOrganizationId(organizationId: String): List<InventoryReorderRule>

    fun findByOrganizationIdAndProductIdAndWarehouseId(
        organizationId: String,
        productId: String,
        warehouseId: String,
    ): Optional<InventoryReorderRule>
}
