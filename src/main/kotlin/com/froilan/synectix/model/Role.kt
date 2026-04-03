package com.froilan.synectix.model

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime
import java.util.UUID

enum class RoleLevel {
    SYSTEM,
    ORGANIZATION,
}

@Document(collection = "roles")
data class Role(
    @Id
    val uuid: String = UUID.randomUUID().toString(),
    @Indexed(unique = true)
    val name: String,
    val description: String,
    val level: RoleLevel,
    val isDefault: Boolean = false,
    val permissions: List<String> = emptyList(),
    @CreatedDate
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    var updatedAt: LocalDateTime? = null,
)
