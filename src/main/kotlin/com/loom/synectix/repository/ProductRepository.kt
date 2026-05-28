package com.loom.synectix.repository

import com.loom.synectix.model.Product
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ProductRepository :
    JpaRepository<Product, String>,
    ProductQueries
