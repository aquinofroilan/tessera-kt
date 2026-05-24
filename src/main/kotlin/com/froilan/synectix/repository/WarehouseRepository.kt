package com.froilan.synectix.repository

import com.froilan.synectix.model.Warehouse
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface WarehouseRepository :
    MongoRepository<Warehouse, String>,
    WarehouseQueries
