package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.Routing
import com.aquinofroilan.tessera.model.RoutingStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface RoutingRepository : JpaRepository<Routing, String> {
    fun findByOrganizationId(organizationId: String): List<Routing>

    fun findByOrganizationIdAndStatus(
        organizationId: String,
        status: RoutingStatus,
    ): List<Routing>

    fun findByOrganizationIdAndProductId(
        organizationId: String,
        productId: String,
    ): List<Routing>

    fun findByOrganizationIdAndProductIdAndStatus(
        organizationId: String,
        productId: String,
        status: RoutingStatus,
    ): List<Routing>

    fun findByOrganizationIdAndProductIdAndIsDefaultTrue(
        organizationId: String,
        productId: String,
    ): Optional<Routing>

    fun findByOrganizationIdAndCode(
        organizationId: String,
        code: String,
    ): Optional<Routing>
}
