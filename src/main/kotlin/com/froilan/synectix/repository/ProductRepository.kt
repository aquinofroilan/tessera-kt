package com.froilan.synectix.repository

import com.froilan.synectix.model.Product
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface ProductRepository : MongoRepository<Product, String> {
    fun findByOrganizationId(organizationId: String): List<Product>

    fun findByOrganizationIdAndIsActive(
        organizationId: String,
        isActive: Boolean,
    ): List<Product>

    fun findByOrganizationIdAndCategory(
        organizationId: String,
        category: String,
    ): List<Product>

    fun findByOrganizationIdAndCategoryAndIsActive(
        organizationId: String,
        category: String,
        isActive: Boolean,
    ): List<Product>
}
