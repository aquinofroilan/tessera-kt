package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.ProductStandardCost
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface ProductStandardCostRepository : JpaRepository<ProductStandardCost, String> {
    fun findByOrganizationIdAndProductId(
        organizationId: String,
        productId: String,
    ): Optional<ProductStandardCost>

    fun findByOrganizationId(organizationId: String): List<ProductStandardCost>
}
