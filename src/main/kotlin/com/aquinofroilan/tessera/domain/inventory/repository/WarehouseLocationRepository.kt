package com.aquinofroilan.tessera.domain.inventory.repository

import com.aquinofroilan.tessera.domain.inventory.model.LocationType
import com.aquinofroilan.tessera.domain.inventory.model.WarehouseLocation
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface WarehouseLocationRepository : JpaRepository<WarehouseLocation, UUID> {
    @Query(
        """
        SELECT l FROM WarehouseLocation l
        WHERE l.organizationId = :organizationId
          AND l.warehouseId = :warehouseId
          AND (:parentLocationId IS NULL OR l.parentLocationId = :parentLocationId)
          AND (:type IS NULL OR l.type = :type)
          AND (:isActive IS NULL OR l.isActive = :isActive)
        """,
    )
    fun search(
        @Param("organizationId") organizationId: UUID,
        @Param("warehouseId") warehouseId: UUID,
        @Param("parentLocationId") parentLocationId: UUID?,
        @Param("type") type: LocationType?,
        @Param("isActive") isActive: Boolean?,
        pageable: Pageable,
    ): Page<WarehouseLocation>

    fun findByOrganizationIdAndWarehouseId(
        organizationId: UUID,
        warehouseId: UUID,
    ): List<WarehouseLocation>

    fun findByOrganizationIdAndWarehouseIdAndIsActiveTrue(
        organizationId: UUID,
        warehouseId: UUID,
    ): List<WarehouseLocation>

    fun existsByOrganizationIdAndWarehouseIdAndCode(
        organizationId: UUID,
        warehouseId: UUID,
        code: String,
    ): Boolean

    fun existsByOrganizationIdAndWarehouseIdAndCodeAndIdNot(
        organizationId: UUID,
        warehouseId: UUID,
        code: String,
        id: UUID,
    ): Boolean

    fun existsByOrganizationIdAndWarehouseIdAndParentLocationIdAndIsActiveTrue(
        organizationId: UUID,
        warehouseId: UUID,
        parentLocationId: UUID,
    ): Boolean
}
