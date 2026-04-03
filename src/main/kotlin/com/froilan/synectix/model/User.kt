package com.froilan.synectix.model

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime
import java.util.UUID

@Document(collection = "users")
data class User(
    @Id
    val uuid: String = UUID.randomUUID().toString(),
    @Indexed(unique = true)
    val username: String,
    @Indexed(unique = true)
    val email: String,
    val firstName: String,
    val lastName: String,
    val passwordHash: String,
    val isActive: Boolean = true,
    val organizationId: String,
    val roleAssignments: List<RoleAssignment> = emptyList(),
    @CreatedDate
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    var updatedAt: LocalDateTime? = null,
)

fun User.effectiveRoleNames(): List<String> = roleAssignments.map { it.role }

fun User.orgRoleNames(orgId: String): List<String> = roleAssignments.filter { it.organizationId == orgId }.map { it.role }

fun User.systemRoleNames(): List<String> = roleAssignments.filter { it.organizationId == null }.map { it.role }
