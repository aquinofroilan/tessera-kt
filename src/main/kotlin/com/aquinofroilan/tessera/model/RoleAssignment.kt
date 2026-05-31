package com.aquinofroilan.tessera.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "user_role_assignments")
class RoleAssignment(
    val role: String,
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: String? = null,
    @Id
    @Column(columnDefinition = "uuid")
    val id: String = UUID.randomUUID().toString(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RoleAssignment) return false
        return role == other.role && organizationId == other.organizationId
    }

    override fun hashCode(): Int = 31 * role.hashCode() + (organizationId?.hashCode() ?: 0)

    override fun toString(): String = "RoleAssignment(role='$role', organizationId=$organizationId)"
}
