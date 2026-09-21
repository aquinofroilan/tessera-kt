package com.aquinofroilan.tessera.domain.auth.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "oauth2_codes")
class OAuth2Code(
    @Id
    var code: String,
    @Column(name = "client_id")
    var clientId: String,
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: UUID,
    @Column(name = "user_id", columnDefinition = "uuid")
    var userId: UUID,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var scopes: List<String>,
    @Column(name = "redirect_uri")
    var redirectUri: String,
    @Column(name = "expires_at")
    var expiresAt: LocalDateTime,
    @Column(name = "created_at")
    var createdAt: LocalDateTime = LocalDateTime.now(java.time.ZoneOffset.UTC),
)
