package com.froilan.synectix.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@Document(collection = "session_tokens")
data class SessionToken(
    @Id
    val id: String = UUID.randomUUID().toString(),
    @Indexed(unique = true)
    val token: String,
    val userId: String,
    val organizationId: String? = null,
    @Indexed(expireAfterSeconds = 0)
    val expiryAt: LocalDateTime,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
)
