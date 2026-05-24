package com.froilan.synectix.repository

import com.froilan.synectix.model.Product
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ProductRepository :
    JpaRepository<Product, String>,
    ProductQueries
