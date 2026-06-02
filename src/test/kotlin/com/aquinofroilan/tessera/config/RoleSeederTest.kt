package com.aquinofroilan.tessera.config

import com.aquinofroilan.tessera.model.Role
import com.aquinofroilan.tessera.model.RoleLevel
import com.aquinofroilan.tessera.repository.RoleRepository
import com.aquinofroilan.tessera.security.Permissions
import com.aquinofroilan.tessera.security.RolePermissionCache
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.springframework.boot.ApplicationArguments
import java.util.Optional

class RoleSeederTest {
    private lateinit var roleRepository: RoleRepository
    private lateinit var rolePermissionCache: RolePermissionCache
    private lateinit var roleSeeder: RoleSeeder

    @BeforeEach
    fun setup() {
        roleRepository = mock(RoleRepository::class.java)
        rolePermissionCache = mock(RolePermissionCache::class.java)
        roleSeeder = RoleSeeder(roleRepository, rolePermissionCache)
    }

    @Test
    fun `should seed all default roles when none exist`() {
        `when`(roleRepository.findByName(any())).thenReturn(Optional.empty())
        `when`(roleRepository.save(any<Role>())).thenAnswer { it.arguments[0] }

        roleSeeder.run(mock(ApplicationArguments::class.java))

        val captor = argumentCaptor<Role>()
        verify(roleRepository, times(5)).save(captor.capture())

        val seededNames = captor.allValues.map { it.name }.toSet()
        assertThat(seededNames).isEqualTo(setOf("SUPER_ADMIN", "OWNER", "ADMIN", "MEMBER", "VIEWER"))
    }

    @Test
    fun `should skip existing role when permissions are already correct`() {
        val existingOwner =
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
                        Permissions.BANK_READ,
                        Permissions.BANK_WRITE,
                        Permissions.BANK_APPROVE,
                        Permissions.ATTACHMENT_READ,
                        Permissions.ATTACHMENT_WRITE,
                        Permissions.HR_RECRUITMENT_READ,
                        Permissions.HR_RECRUITMENT_WRITE,
                        Permissions.HR_RECRUITMENT_APPROVE,
                        Permissions.MFG_READ,
                        Permissions.MFG_WRITE,
                        Permissions.MFG_APPROVE,
                        Permissions.PROJECT_READ,
                        Permissions.PROJECT_WRITE,
                        Permissions.PROJECT_APPROVE,
                        Permissions.NOTIFICATION_WRITE,
                        Permissions.CRM_READ,
                        Permissions.CRM_WRITE,
                        Permissions.WORKFLOW_MANAGE,
                        Permissions.ASSETS_READ,
                        Permissions.ASSETS_WRITE,
                        Permissions.ASSETS_APPROVE,
                    ),
            )
        `when`(roleRepository.findByName(any())).thenReturn(Optional.empty())
        `when`(roleRepository.findByName("OWNER")).thenReturn(Optional.of(existingOwner))
        `when`(roleRepository.save(any<Role>())).thenAnswer { it.arguments[0] }

        roleSeeder.run(mock(ApplicationArguments::class.java))

        val captor = argumentCaptor<Role>()
        verify(roleRepository, times(4)).save(captor.capture())
        assertThat(captor.allValues).noneMatch { it.name == "OWNER" }
    }

    @Test
    fun `should update permissions when role exists with outdated permissions`() {
        val outdatedRole =
            Role(
                name = "MEMBER",
                description = "Standard organization member",
                level = RoleLevel.ORGANIZATION,
                permissions = emptyList(),
            )
        `when`(roleRepository.findByName(any())).thenReturn(Optional.empty())
        `when`(roleRepository.findByName("MEMBER")).thenReturn(Optional.of(outdatedRole))
        `when`(roleRepository.save(any<Role>())).thenAnswer { it.arguments[0] }

        roleSeeder.run(mock(ApplicationArguments::class.java))

        val captor = argumentCaptor<Role>()
        verify(roleRepository, times(5)).save(captor.capture())

        val updatedMember = captor.allValues.first { it.name == "MEMBER" }
        assertThat(updatedMember.permissions).contains(Permissions.SESSION_READ)
        assertThat(updatedMember.permissions).contains(Permissions.ORGANIZATION_READ)
    }

    @Test
    fun `should update all canonical fields when role drifted in DB`() {
        val driftedRole =
            Role(
                name = "OWNER",
                description = "Modified description",
                level = RoleLevel.SYSTEM,
                isDefault = false,
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
                    ),
            )
        `when`(roleRepository.findByName(any())).thenReturn(Optional.empty())
        `when`(roleRepository.findByName("OWNER")).thenReturn(Optional.of(driftedRole))
        `when`(roleRepository.save(any<Role>())).thenAnswer { it.arguments[0] }

        roleSeeder.run(mock(ApplicationArguments::class.java))

        val captor = argumentCaptor<Role>()
        verify(roleRepository, times(5)).save(captor.capture())

        val updatedOwner = captor.allValues.first { it.name == "OWNER" }
        assertThat(updatedOwner.description).isEqualTo("Organization owner with full org-level access")
        assertThat(updatedOwner.level).isEqualTo(RoleLevel.ORGANIZATION)
        assertThat(updatedOwner.isDefault).isTrue()
    }

    @Test
    fun `should seed SUPER_ADMIN as system-level role with no permissions`() {
        `when`(roleRepository.findByName(any())).thenReturn(Optional.empty())
        `when`(roleRepository.save(any<Role>())).thenAnswer { it.arguments[0] }

        roleSeeder.run(mock(ApplicationArguments::class.java))

        val captor = argumentCaptor<Role>()
        verify(roleRepository, times(5)).save(captor.capture())

        val superAdmin = captor.allValues.first { it.name == "SUPER_ADMIN" }
        assertThat(superAdmin.level).isEqualTo(RoleLevel.SYSTEM)
        assertThat(superAdmin.permissions).isEmpty()
    }

    @Test
    fun `should seed org-level roles with correct level`() {
        `when`(roleRepository.findByName(any())).thenReturn(Optional.empty())
        `when`(roleRepository.save(any<Role>())).thenAnswer { it.arguments[0] }

        roleSeeder.run(mock(ApplicationArguments::class.java))

        val captor = argumentCaptor<Role>()
        verify(roleRepository, times(5)).save(captor.capture())

        val orgRoles = captor.allValues.filter { it.name in setOf("OWNER", "ADMIN", "MEMBER", "VIEWER") }
        assertThat(orgRoles).allMatch { it.level == RoleLevel.ORGANIZATION }
    }

    @Test
    fun `should refresh cache after seeding`() {
        `when`(roleRepository.findByName(any())).thenReturn(Optional.empty())
        `when`(roleRepository.save(any<Role>())).thenAnswer { it.arguments[0] }

        roleSeeder.run(mock(ApplicationArguments::class.java))

        verify(rolePermissionCache).refresh()
    }

    @Test
    fun `should not refresh cache when nothing changed`() {
        val superAdmin =
            Role(
                name = "SUPER_ADMIN",
                description = "System-wide administrator with full access",
                level = RoleLevel.SYSTEM,
                permissions = emptyList(),
            )
        val owner =
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
                        Permissions.BANK_READ,
                        Permissions.BANK_WRITE,
                        Permissions.BANK_APPROVE,
                        Permissions.ATTACHMENT_READ,
                        Permissions.ATTACHMENT_WRITE,
                        Permissions.HR_RECRUITMENT_READ,
                        Permissions.HR_RECRUITMENT_WRITE,
                        Permissions.HR_RECRUITMENT_APPROVE,
                        Permissions.MFG_READ,
                        Permissions.MFG_WRITE,
                        Permissions.MFG_APPROVE,
                        Permissions.PROJECT_READ,
                        Permissions.PROJECT_WRITE,
                        Permissions.PROJECT_APPROVE,
                        Permissions.NOTIFICATION_WRITE,
                        Permissions.CRM_READ,
                        Permissions.CRM_WRITE,
                        Permissions.WORKFLOW_MANAGE,
                        Permissions.ASSETS_READ,
                        Permissions.ASSETS_WRITE,
                        Permissions.ASSETS_APPROVE,
                    ),
            )
        val admin =
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
                        Permissions.BANK_READ,
                        Permissions.BANK_WRITE,
                        Permissions.BANK_APPROVE,
                        Permissions.ATTACHMENT_READ,
                        Permissions.ATTACHMENT_WRITE,
                        Permissions.HR_RECRUITMENT_READ,
                        Permissions.HR_RECRUITMENT_WRITE,
                        Permissions.HR_RECRUITMENT_APPROVE,
                        Permissions.MFG_READ,
                        Permissions.MFG_WRITE,
                        Permissions.MFG_APPROVE,
                        Permissions.PROJECT_READ,
                        Permissions.PROJECT_WRITE,
                        Permissions.PROJECT_APPROVE,
                        Permissions.NOTIFICATION_WRITE,
                        Permissions.CRM_READ,
                        Permissions.CRM_WRITE,
                        Permissions.WORKFLOW_MANAGE,
                        Permissions.ASSETS_READ,
                        Permissions.ASSETS_WRITE,
                    ),
            )
        val member =
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
                        Permissions.BANK_READ,
                        Permissions.BANK_WRITE,
                        Permissions.ATTACHMENT_READ,
                        Permissions.ATTACHMENT_WRITE,
                        Permissions.HR_RECRUITMENT_READ,
                        Permissions.HR_RECRUITMENT_WRITE,
                        Permissions.MFG_READ,
                        Permissions.MFG_WRITE,
                        Permissions.PROJECT_READ,
                        Permissions.PROJECT_WRITE,
                        Permissions.CRM_READ,
                        Permissions.CRM_WRITE,
                        Permissions.ASSETS_READ,
                    ),
            )
        val viewer =
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
                        Permissions.BANK_READ,
                        Permissions.ATTACHMENT_READ,
                        Permissions.HR_RECRUITMENT_READ,
                        Permissions.MFG_READ,
                        Permissions.PROJECT_READ,
                        Permissions.CRM_READ,
                        Permissions.ASSETS_READ,
                    ),
            )

        `when`(roleRepository.findByName("SUPER_ADMIN")).thenReturn(Optional.of(superAdmin))
        `when`(roleRepository.findByName("OWNER")).thenReturn(Optional.of(owner))
        `when`(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(admin))
        `when`(roleRepository.findByName("MEMBER")).thenReturn(Optional.of(member))
        `when`(roleRepository.findByName("VIEWER")).thenReturn(Optional.of(viewer))

        roleSeeder.run(mock(ApplicationArguments::class.java))

        verify(roleRepository, never()).save(any<Role>())
        verify(rolePermissionCache, never()).refresh()
    }
}
