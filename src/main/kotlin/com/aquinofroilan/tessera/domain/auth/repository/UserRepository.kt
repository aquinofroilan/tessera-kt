package com.aquinofroilan.tessera.domain.auth.repository

import com.aquinofroilan.tessera.domain.auth.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface UserRepository : JpaRepository<User, java.util.UUID> {
    fun findByUsername(username: String): Optional<User>

    fun findByEmail(email: String): Optional<User>

    fun findByEmailIgnoreCase(email: String): Optional<User>

    fun existsByUsername(username: String): Boolean

    fun existsByEmail(email: String): Boolean

    @org.springframework.data.jpa.repository.Query(
        "SELECT DISTINCT u FROM User u JOIN u.roleAssignments r WHERE r.organizationId = :organizationId AND r.role IN :roles",
    )
    fun findByOrganizationIdAndRolesIn(
        @org.springframework.data.repository.query.Param("organizationId") organizationId: java.util.UUID,
        @org.springframework.data.repository.query.Param("roles") roles: List<String>,
    ): List<User>
}
