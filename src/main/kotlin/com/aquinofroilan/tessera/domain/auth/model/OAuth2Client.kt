package com.aquinofroilan.tessera.domain.auth.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "oauth2_clients")
@EntityListeners(AuditingEntityListener::class)
class OAuth2Client(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID(),
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: UUID,
    @Column(name = "client_id")
    var clientId: String,
    @Column(name = "client_secret_hash")
    var clientSecretHash: String,
    var name: String,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "redirect_uris", columnDefinition = "jsonb")
    var redirectUris: List<String>,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "allowed_scopes", columnDefinition = "jsonb")
    var allowedScopes: List<String>,
    @Column(name = "is_active")
    var isActive: Boolean = true,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)
