package com.aquinofroilan.tessera.domain.inventory.repository

import com.aquinofroilan.tessera.domain.inventory.model.StockMovement
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface StockMovementRepository :
    JpaRepository<StockMovement, java.util.UUID>,
    StockMovementQueries {
    fun findByOrganizationIdAndReference(
        organizationId: java.util.UUID,
        reference: String,
    ): List<StockMovement>
}
