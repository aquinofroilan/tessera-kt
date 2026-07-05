package com.aquinofroilan.tessera.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@Entity
@Table(name = "password_reset_tokens")
class PasswordResetToken(
    @Id
    @Column(columnDefinition = "uuid")
    var id: String = UUID.randomUUID().toString(),
    @Column(name = "token_hash")
    var tokenHash: String,
    @Column(name = "user_id", columnDefinition = "uuid")
    var userId: String,
    @Column(name = "expiry_at")
    var expiryAt: LocalDateTime,
    @Column(name = "created_at")
    var createdAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
)
