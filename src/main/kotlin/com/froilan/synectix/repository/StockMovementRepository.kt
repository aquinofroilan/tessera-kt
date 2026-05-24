package com.froilan.synectix.repository

import com.froilan.synectix.model.StockMovement
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface StockMovementRepository :
    JpaRepository<StockMovement, String>,
    StockMovementQueries
