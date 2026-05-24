package com.froilan.synectix.repository

import com.froilan.synectix.model.Warehouse
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface WarehouseRepository :
    JpaRepository<Warehouse, String>,
    WarehouseQueries
