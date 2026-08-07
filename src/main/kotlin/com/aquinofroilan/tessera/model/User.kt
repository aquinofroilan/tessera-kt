package com.aquinofroilan.tessera.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener::class)
class User(
    @Id
    @Column(name = "uuid", columnDefinition = "uuid")
    var uuid: java.util.UUID = java.util.UUID.ofEpochMillis(System.currentTimeMillis()),
    var username: String,
    var email: String,
    @Column(name = "first_name")
    var firstName: String,
    @Column(name = "last_name")
    var lastName: String,
    @Column(name = "password_hash")
    var passwordHash: String,
    @Column(name = "is_active")
    var isActive: Boolean = true,
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: java.util.UUID,
    @OneToMany(
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.EAGER,
    )
    @JoinColumn(name = "user_id")
    var roleAssignments: List<RoleAssignment> = emptyList(),
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)

fun User.effectiveRoleNames(): List<String> = roleAssignments.map { it.role }.distinct()

fun User.orgRoleNames(orgId: java.util.UUID): List<String> = roleAssignments.filter { it.organizationId == orgId }.map { it.role }

fun User.systemRoleNames(): List<String> = roleAssignments.filter { it.organizationId == null }.map { it.role }
