package com.loom.synectix.repository

import com.loom.synectix.model.StockMovement
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface StockMovementRepository :
    JpaRepository<StockMovement, String>,
    StockMovementQueries
