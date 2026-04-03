package com.froilan.synectix.config

import com.froilan.synectix.model.Role
import com.froilan.synectix.model.RoleLevel
import com.froilan.synectix.repository.RoleRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(1)
class RoleSeeder(
    private val roleRepository: RoleRepository,
) : ApplicationRunner {
    private val log = LoggerFactory.getLogger(RoleSeeder::class.java)

    override fun run(args: ApplicationArguments) {
        val defaultRoles =
            listOf(
                Role(
                    name = "SUPER_ADMIN",
                    description = "System-wide administrator with full access",
                    level = RoleLevel.SYSTEM,
                ),
                Role(
                    name = "OWNER",
                    description = "Organization owner with full org-level access",
                    level = RoleLevel.ORGANIZATION,
                    isDefault = true,
                ),
                Role(
                    name = "ADMIN",
                    description = "Organization administrator",
                    level = RoleLevel.ORGANIZATION,
                ),
                Role(
                    name = "MEMBER",
                    description = "Standard organization member",
                    level = RoleLevel.ORGANIZATION,
                ),
                Role(
                    name = "VIEWER",
                    description = "Read-only organization access",
                    level = RoleLevel.ORGANIZATION,
                ),
            )

        defaultRoles.forEach { role ->
            if (!roleRepository.existsByName(role.name)) {
                roleRepository.save(role)
                log.info("Seeded role: {} ({})", role.name, role.level)
            }
        }
    }
}
