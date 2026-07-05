package com.aquinofroilan.tessera.model

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
    var id: String = UUID.randomUUID().toString(),
    var token: String,
    @Column(name = "user_id", columnDefinition = "uuid")
    var userId: String,
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: String? = null,
    @Column(name = "expiry_at")
    var expiryAt: LocalDateTime,
    @Column(name = "ip_address")
    var ipAddress: String? = null,
    @Column(name = "user_agent")
    var userAgent: String? = null,
    @Column(name = "created_at")
    var createdAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
)
