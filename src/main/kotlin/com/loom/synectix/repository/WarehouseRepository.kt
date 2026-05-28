package com.loom.synectix.repository

import com.loom.synectix.model.Warehouse
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface WarehouseRepository :
    JpaRepository<Warehouse, String>,
    WarehouseQueries
