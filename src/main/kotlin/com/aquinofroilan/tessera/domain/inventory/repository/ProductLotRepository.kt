package com.aquinofroilan.tessera.domain.inventory.repository

import com.aquinofroilan.tessera.domain.inventory.model.ProductLot
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ProductLotRepository : JpaRepository<ProductLot, UUID> {
    fun findByOrganizationIdAndProductIdAndLotNumber(
        organizationId: UUID,
        productId: UUID,
        lotNumber: String,
    ): ProductLot?
}
