package com.aquinofroilan.tessera.domain.mfg.repository

import com.aquinofroilan.tessera.domain.mfg.model.BillOfMaterials
import com.aquinofroilan.tessera.domain.mfg.model.BomStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface BillOfMaterialsRepository : JpaRepository<BillOfMaterials, java.util.UUID> {
    fun findByOrganizationId(organizationId: java.util.UUID): List<BillOfMaterials>

    fun findByOrganizationIdAndStatus(
        organizationId: java.util.UUID,
        status: BomStatus,
    ): List<BillOfMaterials>

    fun findByOrganizationIdAndProductId(
        organizationId: java.util.UUID,
        productId: java.util.UUID,
    ): List<BillOfMaterials>

    fun findByOrganizationIdAndProductIdAndStatus(
        organizationId: java.util.UUID,
        productId: java.util.UUID,
        status: BomStatus,
    ): List<BillOfMaterials>

    fun findByOrganizationIdAndProductIdAndIsDefaultTrue(
        organizationId: java.util.UUID,
        productId: java.util.UUID,
    ): Optional<BillOfMaterials>

    fun findByOrganizationIdAndCode(
        organizationId: java.util.UUID,
        code: String,
    ): Optional<BillOfMaterials>
}
