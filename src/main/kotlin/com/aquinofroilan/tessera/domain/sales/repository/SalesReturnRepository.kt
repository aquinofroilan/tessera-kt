package com.aquinofroilan.tessera.domain.sales.repository

import com.aquinofroilan.tessera.domain.sales.model.SalesReturn
import com.aquinofroilan.tessera.domain.sales.model.SalesReturnStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface SalesReturnRepository : JpaRepository<SalesReturn, UUID> {
    fun findByOrganizationId(organizationId: UUID): List<SalesReturn>

    fun findByIdAndOrganizationId(
        id: UUID,
        organizationId: UUID,
    ): Optional<SalesReturn>

    fun findByOrganizationIdAndCustomerId(
        organizationId: UUID,
        customerId: UUID,
    ): List<SalesReturn>

    fun findByOrganizationIdAndStatus(
        organizationId: UUID,
        status: SalesReturnStatus,
    ): List<SalesReturn>

    fun countByOrganizationId(organizationId: UUID): Long

    fun existsByOrganizationIdAndReturnNumber(
        organizationId: UUID,
        returnNumber: String,
    ): Boolean
}
