package com.aquinofroilan.tessera.model

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "api_keys")
@EntityListeners(AuditingEntityListener::class)
class ApiKey(
    @Id
    @Column(columnDefinition = "uuid")
    var id: String = UUID.randomUUID().toString(),
    var name: String,
    @Column(name = "key_hash")
    var keyHash: String,
    @Column(name = "key_prefix")
    var keyPrefix: String,
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: String,
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "api_key_permissions",
        joinColumns = [JoinColumn(name = "api_key_id")],
    )
    @Column(name = "permission")
    var permissions: List<String>,
    @Column(name = "created_by", columnDefinition = "uuid")
    var createdBy: String,
    @Column(name = "is_active")
    var isActive: Boolean = true,
    @Column(name = "last_used_at")
    var lastUsedAt: LocalDateTime? = null,
    @Column(name = "expires_at")
    var expiresAt: LocalDateTime? = null,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
)
