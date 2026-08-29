package com.aquinofroilan.tessera.domain.organization.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@Entity
@Table(
    name = "organization_feature_flags",
    uniqueConstraints = [UniqueConstraint(columnNames = ["organization_id", "feature_key"])],
)
@EntityListeners(AuditingEntityListener::class)
class OrganizationFeatureFlag(
    @Id
    @Column(name = "id", columnDefinition = "uuid")
    var id: UUID = UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "organization_id", nullable = false)
    var organizationId: UUID,
    @Column(name = "feature_key", nullable = false)
    var featureKey: String,
    @Column(name = "enabled", nullable = false)
    var enabled: Boolean,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
)
