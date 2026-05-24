package com.froilan.synectix.repository

import com.froilan.synectix.model.Product
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface ProductRepository :
    MongoRepository<Product, String>,
    ProductQueries
