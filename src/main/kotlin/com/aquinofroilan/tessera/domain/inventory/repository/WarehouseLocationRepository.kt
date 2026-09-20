package com.aquinofroilan.tessera.domain.inventory.repository

import com.aquinofroilan.tessera.domain.inventory.model.WarehouseLocation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface WarehouseLocationRepository : JpaRepository<WarehouseLocation, UUID> {
    fun findByOrganizationIdAndWarehouseId(
        organizationId: UUID,
        warehouseId: UUID,
    ): List<WarehouseLocation>

    fun existsByOrganizationIdAndWarehouseIdAndCode(
        organizationId: UUID,
        warehouseId: UUID,
        code: String,
    ): Boolean
}
