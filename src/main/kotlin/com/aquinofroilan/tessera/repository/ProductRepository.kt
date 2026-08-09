package com.aquinofroilan.tessera.repository

import java.util.UUID

import com.aquinofroilan.tessera.model.Product
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ProductRepository :
    JpaRepository<Product, java.util.UUID>,
    ProductQueries
