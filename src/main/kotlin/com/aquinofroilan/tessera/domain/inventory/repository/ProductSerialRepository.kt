package com.aquinofroilan.tessera.domain.inventory.repository

import com.aquinofroilan.tessera.domain.inventory.model.ProductSerial
import com.aquinofroilan.tessera.domain.inventory.model.SerialStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ProductSerialRepository : JpaRepository<ProductSerial, UUID> {
    fun findByOrganizationIdAndProductIdAndSerialNumber(
        organizationId: UUID,
        productId: UUID,
        serialNumber: String,
    ): ProductSerial?

    fun findByOrganizationIdAndProductIdAndSerialNumberIn(
        organizationId: UUID,
        productId: UUID,
        serialNumbers: List<String>,
    ): List<ProductSerial>

    fun findByOrganizationIdAndProductIdAndStatusAndCurrentWarehouseId(
        organizationId: UUID,
        productId: UUID,
        status: SerialStatus,
        currentWarehouseId: UUID,
    ): List<ProductSerial>
}
