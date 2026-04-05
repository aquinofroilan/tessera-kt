package com.froilan.synectix.model

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime
import java.util.UUID

@Document(collection = "api_keys")
data class ApiKey(
    @Id
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    @Indexed(unique = true)
    val keyHash: String,
    val keyPrefix: String,
    @Indexed
    val organizationId: String,
    val permissions: List<String>,
    val createdBy: String,
    val isActive: Boolean = true,
    val lastUsedAt: LocalDateTime? = null,
    val expiresAt: LocalDateTime? = null,
    @CreatedDate
    var createdAt: LocalDateTime? = null,
)
