package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.Warehouse
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface WarehouseRepository :
    JpaRepository<Warehouse, String>,
    WarehouseQueries
