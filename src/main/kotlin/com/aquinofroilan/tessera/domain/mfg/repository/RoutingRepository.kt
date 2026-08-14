package com.aquinofroilan.tessera.domain.mfg.repository

import com.aquinofroilan.tessera.domain.mfg.model.Routing
import com.aquinofroilan.tessera.domain.mfg.model.RoutingStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface RoutingRepository : JpaRepository<Routing, java.util.UUID> {
    fun findByOrganizationId(organizationId: java.util.UUID): List<Routing>

    fun findByOrganizationIdAndStatus(
        organizationId: java.util.UUID,
        status: RoutingStatus,
    ): List<Routing>

    fun findByOrganizationIdAndProductId(
        organizationId: java.util.UUID,
        productId: java.util.UUID,
    ): List<Routing>

    fun findByOrganizationIdAndProductIdAndStatus(
        organizationId: java.util.UUID,
        productId: java.util.UUID,
        status: RoutingStatus,
    ): List<Routing>

    fun findByOrganizationIdAndProductIdAndIsDefaultTrue(
        organizationId: java.util.UUID,
        productId: java.util.UUID,
    ): Optional<Routing>

    fun findByOrganizationIdAndCode(
        organizationId: java.util.UUID,
        code: String,
    ): Optional<Routing>
}
