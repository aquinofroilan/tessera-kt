package com.froilan.synectix.repository

import com.froilan.synectix.model.Role
import com.froilan.synectix.model.RoleLevel
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface RoleRepository : MongoRepository<Role, String> {
    fun findByName(name: String): Optional<Role>

    fun findByLevel(level: RoleLevel): List<Role>

    fun existsByName(name: String): Boolean
}
