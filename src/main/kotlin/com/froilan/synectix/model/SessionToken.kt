package com.froilan.synectix.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.index.Indexed
import java.time.LocalDateTime
import java.util.UUID

@Document(collection = "session_tokens")
data class SessionToken(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @Indexed(unique = true)
    val token: String,

    val userId: String,

    val expiryAt: LocalDateTime,

    val createdAt: LocalDateTime = LocalDateTime.now()
)
