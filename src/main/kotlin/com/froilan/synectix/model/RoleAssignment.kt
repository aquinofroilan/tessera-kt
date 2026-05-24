package com.froilan.synectix.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "user_role_assignments")
data class RoleAssignment(
    @Id
    @Column(columnDefinition = "uuid")
    val id: String = UUID.randomUUID().toString(),
    val role: String,
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: String? = null,
)
