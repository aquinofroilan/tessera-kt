package com.froilan.synectix.repository

import com.froilan.synectix.model.StockMovement
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface StockMovementRepository :
    MongoRepository<StockMovement, String>,
    StockMovementQueries
