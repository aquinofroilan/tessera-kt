package com.aquinofroilan.tessera.domain.organization.service

import com.aquinofroilan.tessera.domain.auth.model.User
import com.aquinofroilan.tessera.domain.organization.dto.AuditLogResponse
import com.aquinofroilan.tessera.domain.organization.model.AuditCategory
import com.aquinofroilan.tessera.domain.organization.model.OrganizationAuditLog
import com.aquinofroilan.tessera.domain.organization.repository.OrganizationAuditLogRepository
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.security.ApiKeyPrincipal
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class OrganizationAuditService(
    private val auditLogRepository: OrganizationAuditLogRepository,
    private val objectMapper: ObjectMapper,
    private val authContext: AuthenticationContext,
) {
    private val log = LoggerFactory.getLogger(OrganizationAuditService::class.java)

    @Transactional
    fun logEvent(
        organizationId: UUID,
        action: String,
        category: AuditCategory,
        entityType: String,
        entityId: String? = null,
        oldValue: Any? = null,
        newValue: Any? = null,
        actorId: UUID? = null,
        actorName: String? = null,
        ipAddress: String? = null,
        userAgent: String? = null,
    ): OrganizationAuditLog {
        val resolvedActorId = actorId ?: authContext.userId()
        val resolvedActorName = actorName ?: resolveCurrentActorName()

        val oldStr = serializeValue(oldValue)
        val newStr = serializeValue(newValue)

        val auditLog =
            OrganizationAuditLog(
                organizationId = organizationId,
                actorId = resolvedActorId,
                actorName = resolvedActorName,
                action = action,
                category = category,
                entityType = entityType,
                entityId = entityId,
                oldValue = oldStr,
                newValue = newStr,
                ipAddress = ipAddress,
                userAgent = userAgent,
            )

        log.debug(
            "Audit event logged: org={}, action={}, actor={}, entity={}:{}",
            organizationId,
            action,
            resolvedActorName,
            entityType,
            entityId,
        )

        return auditLogRepository.save(auditLog)
    }

    @Transactional(readOnly = true)
    fun getAuditLogs(
        organizationId: UUID,
        category: AuditCategory? = null,
        action: String? = null,
        entityType: String? = null,
        from: LocalDateTime? = null,
        to: LocalDateTime? = null,
        pageable: Pageable,
    ): Page<AuditLogResponse> {
        val spec =
            Specification<OrganizationAuditLog> { root, _, cb ->
                val predicates = mutableListOf(cb.equal(root.get<UUID>("organizationId"), organizationId))

                category?.let { predicates.add(cb.equal(root.get<AuditCategory>("category"), it)) }
                action?.let { predicates.add(cb.equal(root.get<String>("action"), it)) }
                entityType?.let { predicates.add(cb.equal(root.get<String>("entityType"), it)) }
                from?.let { predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), it)) }
                to?.let { predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), it)) }

                cb.and(*predicates.toTypedArray())
            }

        return auditLogRepository.findAll(spec, pageable).map { AuditLogResponse.from(it) }
    }

    @Transactional(readOnly = true)
    fun getAuditLogById(
        organizationId: UUID,
        id: UUID,
    ): AuditLogResponse {
        val auditLog =
            auditLogRepository.findByOrganizationIdAndId(organizationId, id).orElseThrow {
                ResourceNotFoundException("Audit log $id not found")
            }
        return AuditLogResponse.from(auditLog)
    }

    private fun resolveCurrentActorName(): String? {
        val auth = SecurityContextHolder.getContext().authentication ?: return null
        return when (val principal = auth.principal) {
            is User -> principal.username
            is ApiKeyPrincipal -> "api-key:${principal.apiKeyName}"
            is String -> principal
            else -> auth.name
        }
    }

    private fun serializeValue(value: Any?): String? {
        if (value == null) return null
        if (value is String) return value
        return try {
            objectMapper.writeValueAsString(value)
        } catch (e: Exception) {
            value.toString()
        }
    }
}
