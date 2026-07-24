package com.aquinofroilan.tessera.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@Entity
@Table(name = "login_link_tokens")
class LoginLinkToken(
    @Id
    @Column(columnDefinition = "uuid")
    var id: java.util.UUID = UUID.randomUUID(),
    @Column(name = "token_hash")
    var tokenHash: String,
    @Column(name = "user_id", columnDefinition = "uuid")
    var userId: java.util.UUID,
    @Column(name = "expiry_at")
    var expiryAt: LocalDateTime,
    @Column(name = "consumed_at")
    var consumedAt: LocalDateTime? = null,
    @Column(name = "ip_address")
    var ipAddress: String? = null,
    @Column(name = "user_agent")
    var userAgent: String? = null,
    @Column(name = "created_at")
    var createdAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
)
