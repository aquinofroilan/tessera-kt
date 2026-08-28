package com.aquinofroilan.tessera.domain.organization.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@Entity
@Table(name = "organization_audit_logs")
@EntityListeners(AuditingEntityListener::class)
class OrganizationAuditLog(
    @Id
    @Column(name = "id", columnDefinition = "uuid")
    var id: UUID = UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "organization_id", nullable = false)
    var organizationId: UUID,
    @Column(name = "actor_id")
    var actorId: UUID? = null,
    @Column(name = "actor_name")
    var actorName: String? = null,
    @Column(name = "action", nullable = false)
    var action: String,
    @Column(name = "category", nullable = false)
    @Enumerated(EnumType.STRING)
    var category: AuditCategory = AuditCategory.SETTINGS,
    @Column(name = "entity_type", nullable = false)
    var entityType: String,
    @Column(name = "entity_id")
    var entityId: String? = null,
    @Column(name = "old_value", columnDefinition = "text")
    var oldValue: String? = null,
    @Column(name = "new_value", columnDefinition = "text")
    var newValue: String? = null,
    @Column(name = "ip_address")
    var ipAddress: String? = null,
    @Column(name = "user_agent")
    var userAgent: String? = null,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
)

enum class AuditCategory {
    SETTINGS,
    MEMBERSHIP,
    LIFECYCLE,
    BILLING,
    FEATURE_FLAGS,
}

enum class AuditAction {
    ORG_CREATED,
    ORG_SETTINGS_UPDATED,
    ORG_STATUS_CHANGED,
    BILLING_PLAN_UPDATED,
    FEATURE_FLAG_OVERRIDDEN,
    FEATURE_FLAG_REVERTED,
    FEATURE_FLAGS_BATCH_UPDATED,
    MEMBER_INVITED,
    MEMBER_JOINED,
    MEMBER_ROLE_CHANGED,
    MEMBER_REMOVED,
}
