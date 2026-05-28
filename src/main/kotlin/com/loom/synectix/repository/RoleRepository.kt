package com.loom.synectix.repository

import com.loom.synectix.model.Role
import com.loom.synectix.model.RoleLevel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface RoleRepository : JpaRepository<Role, String> {
    fun findByName(name: String): Optional<Role>

    fun findByLevel(level: RoleLevel): List<Role>

    fun existsByName(name: String): Boolean
}
