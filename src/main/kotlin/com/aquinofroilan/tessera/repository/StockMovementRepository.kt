package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.StockMovement
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface StockMovementRepository :
    JpaRepository<StockMovement, String>,
    StockMovementQueries {
    fun findByOrganizationIdAndReference(
        organizationId: String,
        reference: String,
    ): List<StockMovement>
}
