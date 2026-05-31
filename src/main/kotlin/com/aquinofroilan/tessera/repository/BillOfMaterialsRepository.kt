package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.BillOfMaterials
import com.aquinofroilan.tessera.model.BomStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface BillOfMaterialsRepository : JpaRepository<BillOfMaterials, String> {
    fun findByOrganizationId(organizationId: String): List<BillOfMaterials>

    fun findByOrganizationIdAndStatus(
        organizationId: String,
        status: BomStatus,
    ): List<BillOfMaterials>

    fun findByOrganizationIdAndProductId(
        organizationId: String,
        productId: String,
    ): List<BillOfMaterials>

    fun findByOrganizationIdAndProductIdAndStatus(
        organizationId: String,
        productId: String,
        status: BomStatus,
    ): List<BillOfMaterials>

    fun findByOrganizationIdAndProductIdAndIsDefaultTrue(
        organizationId: String,
        productId: String,
    ): Optional<BillOfMaterials>

    fun findByOrganizationIdAndCode(
        organizationId: String,
        code: String,
    ): Optional<BillOfMaterials>
}
