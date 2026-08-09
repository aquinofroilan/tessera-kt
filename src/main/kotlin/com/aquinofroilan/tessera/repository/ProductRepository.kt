package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.Product
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ProductRepository :
    JpaRepository<Product, java.util.UUID>,
    ProductQueries
