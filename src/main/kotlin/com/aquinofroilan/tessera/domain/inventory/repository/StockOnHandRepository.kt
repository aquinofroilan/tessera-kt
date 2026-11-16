package com.aquinofroilan.tessera.domain.inventory.repository

import com.aquinofroilan.tessera.domain.inventory.model.StockOnHand
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface StockOnHandRepository :
    JpaRepository<StockOnHand, java.util.UUID>,
    StockOnHandQueries {
    fun findByOrganizationId(organizationId: java.util.UUID): List<StockOnHand>

    fun findByOrganizationIdAndWarehouseIdAndLocationId(
        organizationId: java.util.UUID,
        warehouseId: java.util.UUID,
        locationId: java.util.UUID,
    ): List<StockOnHand>

    fun findByOrganizationIdAndProductIdAndWarehouseId(
        organizationId: java.util.UUID,
        productId: java.util.UUID,
        warehouseId: java.util.UUID,
    ): java.util.Optional<StockOnHand>

    @org.springframework.data.jpa.repository.Query(
        "SELECT COALESCE(SUM(s.quantity * p.listPrice), 0) FROM StockOnHand s JOIN Product p ON s.productId = p.id WHERE s.organizationId = :organizationId",
    )
    fun calculateInventoryValuation(
        @org.springframework.data.repository.query.Param("organizationId") organizationId: java.util.UUID,
    ): java.math.BigDecimal
}
