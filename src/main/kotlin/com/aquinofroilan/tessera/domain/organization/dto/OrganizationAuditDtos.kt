package com.aquinofroilan.tessera.domain.organization.dto

import com.aquinofroilan.tessera.domain.organization.model.AuditCategory
import com.aquinofroilan.tessera.domain.organization.model.OrganizationAuditLog
import java.time.LocalDateTime
import java.util.UUID

data class AuditLogResponse(
    val id: UUID,
    val organizationId: UUID,
    val actorId: UUID?,
    val actorName: String?,
    val action: String,
    val category: AuditCategory,
    val entityType: String,
    val entityId: String?,
    val oldValue: String?,
    val newValue: String?,
    val ipAddress: String?,
    val userAgent: String?,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(log: OrganizationAuditLog): AuditLogResponse =
            AuditLogResponse(
                id = log.id,
                organizationId = log.organizationId,
                actorId = log.actorId,
                actorName = log.actorName,
                action = log.action,
                category = log.category,
                entityType = log.entityType,
                entityId = log.entityId,
                oldValue = log.oldValue,
                newValue = log.newValue,
                ipAddress = log.ipAddress,
                userAgent = log.userAgent,
                createdAt = log.createdAt,
            )
    }
}

data class AuditLogFilterCriteria(
    val category: AuditCategory? = null,
    val action: String? = null,
    val entityType: String? = null,
    val from: LocalDateTime? = null,
    val to: LocalDateTime? = null,
)
