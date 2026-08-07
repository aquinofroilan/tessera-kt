package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.Role
import com.aquinofroilan.tessera.model.RoleLevel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface RoleRepository : JpaRepository<Role, java.util.UUID> {
    fun findByName(name: String): Optional<Role>

    fun findByLevel(level: RoleLevel): List<Role>

    fun existsByName(name: String): Boolean
}
