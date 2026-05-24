package com.froilan.synectix.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@Entity
@Table(name = "session_tokens")
data class SessionToken(
    @Id
    @Column(columnDefinition = "uuid")
    val id: String = UUID.randomUUID().toString(),
    val token: String,
    @Column(name = "user_id", columnDefinition = "uuid")
    val userId: String,
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: String? = null,
    @Column(name = "expiry_at")
    val expiryAt: LocalDateTime,
    @Column(name = "ip_address")
    val ipAddress: String? = null,
    @Column(name = "user_agent")
    val userAgent: String? = null,
    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
)
