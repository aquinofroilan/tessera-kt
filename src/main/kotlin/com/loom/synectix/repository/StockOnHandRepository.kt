package com.loom.synectix.repository

import com.loom.synectix.model.StockOnHand
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface StockOnHandRepository :
    JpaRepository<StockOnHand, String>,
    StockOnHandQueries {
    fun findByOrganizationId(organizationId: String): List<StockOnHand>
}
