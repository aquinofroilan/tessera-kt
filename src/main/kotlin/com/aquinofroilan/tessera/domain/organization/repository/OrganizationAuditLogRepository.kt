package com.aquinofroilan.tessera.domain.organization.repository

import com.aquinofroilan.tessera.domain.organization.model.AuditCategory
import com.aquinofroilan.tessera.domain.organization.model.OrganizationAuditLog
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

@Repository
interface OrganizationAuditLogRepository :
    JpaRepository<OrganizationAuditLog, UUID>,
    JpaSpecificationExecutor<OrganizationAuditLog> {
    fun findByOrganizationId(
        organizationId: UUID,
        pageable: Pageable,
    ): Page<OrganizationAuditLog>

    fun findByOrganizationIdAndId(
        organizationId: UUID,
        id: UUID,
    ): Optional<OrganizationAuditLog>

    fun findByOrganizationIdAndCategory(
        organizationId: UUID,
        category: AuditCategory,
        pageable: Pageable,
    ): Page<OrganizationAuditLog>

    fun findByOrganizationIdAndAction(
        organizationId: UUID,
        action: String,
        pageable: Pageable,
    ): Page<OrganizationAuditLog>

    fun findByOrganizationIdAndCreatedAtBetween(
        organizationId: UUID,
        from: LocalDateTime,
        to: LocalDateTime,
        pageable: Pageable,
    ): Page<OrganizationAuditLog>
}
