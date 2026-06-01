package com.aquinofroilan.tessera.config

import com.aquinofroilan.tessera.model.Role
import com.aquinofroilan.tessera.model.RoleLevel
import com.aquinofroilan.tessera.repository.RoleRepository
import com.aquinofroilan.tessera.security.Permissions
import com.aquinofroilan.tessera.security.RolePermissionCache
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.annotation.Order
import org.springframework.dao.DataIntegrityViolationException
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
                            Permissions.AP_CREATE,
                            Permissions.AP_READ,
                            Permissions.AP_APPROVE,
                            Permissions.AP_PAY,
                            Permissions.AP_VOID,
                            Permissions.AR_CREATE,
                            Permissions.AR_READ,
                            Permissions.AR_APPROVE,
                            Permissions.AR_RECEIVE,
                            Permissions.AR_VOID,
                            Permissions.TAX_CREATE,
                            Permissions.TAX_READ,
                            Permissions.TAX_DELETE,
                            Permissions.FX_READ,
                            Permissions.FX_CREATE,
                            Permissions.INVENTORY_READ,
                            Permissions.INVENTORY_WRITE,
                            Permissions.PROCUREMENT_READ,
                            Permissions.PROCUREMENT_WRITE,
                            Permissions.PROCUREMENT_APPROVE,
                            Permissions.PROCUREMENT_RECEIVE,
                            Permissions.SALES_READ,
                            Permissions.SALES_WRITE,
                            Permissions.SALES_APPROVE,
                            Permissions.SALES_FULFILL,
                            Permissions.HR_READ,
                            Permissions.HR_WRITE,
                            Permissions.HR_APPROVE,
                            Permissions.PROJECT_READ,
                            Permissions.PROJECT_WRITE,
                            Permissions.PROJECT_APPROVE,
                            Permissions.NOTIFICATION_WRITE,
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
                            Permissions.AP_CREATE,
                            Permissions.AP_READ,
                            Permissions.AP_APPROVE,
                            Permissions.AP_PAY,
                            Permissions.AR_CREATE,
                            Permissions.AR_READ,
                            Permissions.AR_APPROVE,
                            Permissions.AR_RECEIVE,
                            Permissions.TAX_CREATE,
                            Permissions.TAX_READ,
                            Permissions.TAX_DELETE,
                            Permissions.FX_READ,
                            Permissions.FX_CREATE,
                            Permissions.INVENTORY_READ,
                            Permissions.INVENTORY_WRITE,
                            Permissions.PROCUREMENT_READ,
                            Permissions.PROCUREMENT_WRITE,
                            Permissions.PROCUREMENT_APPROVE,
                            Permissions.PROCUREMENT_RECEIVE,
                            Permissions.SALES_READ,
                            Permissions.SALES_WRITE,
                            Permissions.SALES_APPROVE,
                            Permissions.SALES_FULFILL,
                            Permissions.HR_READ,
                            Permissions.HR_WRITE,
                            Permissions.HR_APPROVE,
                            Permissions.PROJECT_READ,
                            Permissions.PROJECT_WRITE,
                            Permissions.PROJECT_APPROVE,
                            Permissions.NOTIFICATION_WRITE,
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
                            Permissions.AP_CREATE,
                            Permissions.AP_READ,
                            Permissions.AR_CREATE,
                            Permissions.AR_READ,
                            Permissions.TAX_READ,
                            Permissions.FX_READ,
                            Permissions.INVENTORY_READ,
                            Permissions.INVENTORY_WRITE,
                            Permissions.PROCUREMENT_READ,
                            Permissions.PROCUREMENT_WRITE,
                            Permissions.SALES_READ,
                            Permissions.SALES_WRITE,
                            Permissions.HR_READ,
                            Permissions.HR_WRITE,
                            Permissions.PROJECT_READ,
                            Permissions.PROJECT_WRITE,
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
                            Permissions.AP_READ,
                            Permissions.AR_READ,
                            Permissions.TAX_READ,
                            Permissions.FX_READ,
                            Permissions.INVENTORY_READ,
                            Permissions.PROCUREMENT_READ,
                            Permissions.SALES_READ,
                            Permissions.HR_READ,
                            Permissions.PROJECT_READ,
                        ),
                ),
            )

        var changed = false
        defaultRoles.forEach { role ->
            val existing = roleRepository.findByName(role.name)
            if (existing.isEmpty) {
                try {
                    roleRepository.save(role)
                    log.info("Seeded role: {} ({})", role.name, role.level)
                    changed = true
                } catch (e: DataIntegrityViolationException) {
                    // A constraint failed. Only treat as a race-loss if the row is
                    // now present; otherwise this is a real schema/data problem.
                    if (roleRepository.findByName(role.name).isEmpty) {
                        throw e
                    }
                    // Roles were inserted by another startup; our @PostConstruct
                    // cache load ran before that, so flag for refresh.
                    changed = true
                }
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
