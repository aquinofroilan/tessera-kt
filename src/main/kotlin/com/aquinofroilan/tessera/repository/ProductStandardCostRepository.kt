package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.ProductStandardCost
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface ProductStandardCostRepository : JpaRepository<ProductStandardCost, java.util.UUID> {
    fun findByOrganizationIdAndProductId(
        organizationId: java.util.UUID,
        productId: java.util.UUID,
    ): Optional<ProductStandardCost>

    fun findByOrganizationId(organizationId: java.util.UUID): List<ProductStandardCost>
}
