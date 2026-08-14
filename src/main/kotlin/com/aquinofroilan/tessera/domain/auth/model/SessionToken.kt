package com.aquinofroilan.tessera.domain.auth.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@Entity
@Table(name = "session_tokens")
class SessionToken(
    @Id
    @Column(columnDefinition = "uuid")
    var id: java.util.UUID = java.util.UUID.ofEpochMillis(System.currentTimeMillis()),
    var token: String,
    @Column(name = "user_id", columnDefinition = "uuid")
    var userId: java.util.UUID,
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: java.util.UUID? = null,
    @Column(name = "expiry_at")
    var expiryAt: LocalDateTime,
    @Column(name = "ip_address")
    var ipAddress: String? = null,
    @Column(name = "user_agent")
    var userAgent: String? = null,
    @Column(name = "created_at")
    var createdAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
)
