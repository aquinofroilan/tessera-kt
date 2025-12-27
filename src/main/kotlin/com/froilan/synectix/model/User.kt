package com.froilan.synectix.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.index.Indexed
import java.time.LocalDateTime
import java.util.UUID

@Document(collection = "users")
data class User(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @Indexed(unique = true)
    val username: String,

    val passwordHash: String,

    val email: String,

    val roles: Set<String> = setOf("USER"),

    val createdAt: LocalDateTime = LocalDateTime.now(),

    var updatedAt: LocalDateTime = LocalDateTime.now()
)
