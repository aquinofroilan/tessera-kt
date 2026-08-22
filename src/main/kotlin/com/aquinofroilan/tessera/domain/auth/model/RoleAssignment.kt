package com.aquinofroilan.tessera.domain.auth.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "user_role_assignments")
class RoleAssignment(
    var role: String,
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: java.util.UUID? = null,
    @Id
    @Column(columnDefinition = "uuid")
    var id: java.util.UUID = java.util.UUID.ofEpochMillis(System.currentTimeMillis()),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RoleAssignment) return false
        return role == other.role && organizationId == other.organizationId
    }

    override fun hashCode(): Int = 31 * role.hashCode() + (organizationId?.hashCode() ?: 0)

    override fun toString(): String = "RoleAssignment(role='$role', organizationId=$organizationId)"
}
