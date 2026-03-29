package com.froilan.synectix.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime
import java.util.UUID

@Document(collection = "refresh_tokens")
data class RefreshToken(
    @Id
    val id: String = UUID.randomUUID().toString(),
    @Indexed(unique = true)
    val tokenHash: String,
    val userId: String,
    val sessionTokenId: String,
    @Indexed(expireAfterSeconds = 0)
    val expiryAt: LocalDateTime,
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
