package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.StockOnHand
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface StockOnHandRepository :
    JpaRepository<StockOnHand, String>,
    StockOnHandQueries {
    fun findByOrganizationId(organizationId: String): List<StockOnHand>
}
