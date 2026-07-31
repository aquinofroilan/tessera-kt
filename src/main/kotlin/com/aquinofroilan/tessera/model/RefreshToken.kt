package com.aquinofroilan.tessera.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@Entity
@Table(name = "refresh_tokens")
class RefreshToken(
    @Id
    @Column(columnDefinition = "uuid")
    var id: java.util.UUID = java.util.UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "token_hash")
    var tokenHash: String,
    @Column(name = "user_id", columnDefinition = "uuid")
    var userId: java.util.UUID,
    @Column(name = "session_token_id", columnDefinition = "uuid")
    var sessionTokenId: java.util.UUID,
    @Column(name = "expiry_at")
    var expiryAt: LocalDateTime,
    @Column(name = "created_at")
    var createdAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
)
