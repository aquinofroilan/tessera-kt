package com.froilan.synectix.repository

import com.froilan.synectix.model.StockOnHand
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface StockOnHandRepository :
    MongoRepository<StockOnHand, String>,
    StockOnHandQueries {
    fun findByOrganizationId(organizationId: String): List<StockOnHand>
}
