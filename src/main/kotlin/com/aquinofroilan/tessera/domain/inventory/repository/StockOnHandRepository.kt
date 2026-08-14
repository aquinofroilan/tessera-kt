package com.aquinofroilan.tessera.domain.inventory.repository

import com.aquinofroilan.tessera.domain.inventory.model.StockOnHand
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface StockOnHandRepository :
    JpaRepository<StockOnHand, java.util.UUID>,
    StockOnHandQueries {
    fun findByOrganizationId(organizationId: java.util.UUID): List<StockOnHand>
}
