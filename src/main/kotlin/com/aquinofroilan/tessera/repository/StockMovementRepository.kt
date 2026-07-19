package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.StockMovement
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
