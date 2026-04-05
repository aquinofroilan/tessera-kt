package com.froilan.synectix.config

import com.froilan.synectix.model.Role
import com.froilan.synectix.model.RoleLevel
import com.froilan.synectix.repository.RoleRepository
import com.froilan.synectix.security.Permissions
import com.froilan.synectix.security.RolePermissionCache
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(1)
class RoleSeeder(
    private val roleRepository: RoleRepository,
    private val rolePermissionCache: RolePermissionCache,
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
                    permissions =
                        listOf(
                            Permissions.SESSION_READ,
                            Permissions.SESSION_DELETE,
                            Permissions.USER_READ,
                            Permissions.USER_WRITE,
                            Permissions.USER_DELETE,
                            Permissions.ORGANIZATION_READ,
                            Permissions.ORGANIZATION_WRITE,
                            Permissions.ORGANIZATION_DELETE,
                            Permissions.INVITATION_READ,
                            Permissions.INVITATION_WRITE,
                            Permissions.API_KEY_MANAGE,
                            Permissions.ACCOUNT_CREATE,
                            Permissions.ACCOUNT_READ,
                            Permissions.ACCOUNT_UPDATE,
                            Permissions.ACCOUNT_DELETE,
                            Permissions.JOURNAL_CREATE,
                            Permissions.JOURNAL_READ,
                            Permissions.JOURNAL_POST,
                            Permissions.JOURNAL_VOID,
                            Permissions.FISCAL_CREATE,
                            Permissions.FISCAL_READ,
                            Permissions.FISCAL_CLOSE,
                        ),
                ),
                Role(
                    name = "ADMIN",
                    description = "Organization administrator",
                    level = RoleLevel.ORGANIZATION,
                    permissions =
                        listOf(
                            Permissions.SESSION_READ,
                            Permissions.SESSION_DELETE,
                            Permissions.USER_READ,
                            Permissions.USER_WRITE,
                            Permissions.ORGANIZATION_READ,
                            Permissions.ORGANIZATION_WRITE,
                            Permissions.INVITATION_READ,
                            Permissions.INVITATION_WRITE,
                            Permissions.API_KEY_MANAGE,
                            Permissions.ACCOUNT_CREATE,
                            Permissions.ACCOUNT_READ,
                            Permissions.ACCOUNT_UPDATE,
                            Permissions.JOURNAL_CREATE,
                            Permissions.JOURNAL_READ,
                            Permissions.JOURNAL_POST,
                            Permissions.FISCAL_CREATE,
                            Permissions.FISCAL_READ,
                            Permissions.FISCAL_CLOSE,
                        ),
                ),
                Role(
                    name = "MEMBER",
                    description = "Standard organization member",
                    level = RoleLevel.ORGANIZATION,
                    permissions =
                        listOf(
                            Permissions.SESSION_READ,
                            Permissions.SESSION_DELETE,
                            Permissions.USER_READ,
                            Permissions.ORGANIZATION_READ,
                            Permissions.INVITATION_READ,
                            Permissions.ACCOUNT_READ,
                            Permissions.JOURNAL_CREATE,
                            Permissions.JOURNAL_READ,
                            Permissions.FISCAL_READ,
                        ),
                ),
                Role(
                    name = "VIEWER",
                    description = "Read-only organization access",
                    level = RoleLevel.ORGANIZATION,
                    permissions =
                        listOf(
                            Permissions.SESSION_READ,
                            Permissions.ORGANIZATION_READ,
                            Permissions.ACCOUNT_READ,
                            Permissions.JOURNAL_READ,
                            Permissions.FISCAL_READ,
                        ),
                ),
            )

        var changed = false
        defaultRoles.forEach { role ->
            val existing = roleRepository.findByName(role.name)
            if (existing.isEmpty) {
                roleRepository.save(role)
                log.info("Seeded role: {} ({})", role.name, role.level)
                changed = true
            } else {
                val current = existing.get()
                if (current.description != role.description ||
                    current.level != role.level ||
                    current.isDefault != role.isDefault ||
                    current.permissions.toSet() != role.permissions.toSet()
                ) {
                    roleRepository.save(
                        current.copy(
                            description = role.description,
                            level = role.level,
                            isDefault = role.isDefault,
                            permissions = role.permissions,
                        ),
                    )
                    log.info("Updated role: {}", role.name)
                    changed = true
                }
            }
        }

        if (changed) {
            rolePermissionCache.refresh()
        }
    }
}
