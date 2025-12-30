package com.froilan.synectix.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.index.Indexed
import java.time.LocalDateTime
import java.util.UUID

@Document(collection = "users")
data class User(
    @Id
    val uuid: String = UUID.randomUUID().toString(),

    @Indexed(unique = true)
    val username: String,

    val passwordHash: String,

    val email: String,

    val firstName: String,

    val lastName: String,

    val isActive: Boolean = true,

    val organizationId: String,

    val roles: List<String> = listOf("USER"),

    val createdAt: LocalDateTime = LocalDateTime.now(),

    var updatedAt: LocalDateTime = LocalDateTime.now()
)
