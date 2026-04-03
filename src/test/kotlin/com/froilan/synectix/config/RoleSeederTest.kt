package com.froilan.synectix.config

import com.froilan.synectix.model.Role
import com.froilan.synectix.model.RoleLevel
import com.froilan.synectix.repository.RoleRepository
import com.froilan.synectix.security.Permissions
import com.froilan.synectix.security.RolePermissionCache
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
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
        assertEquals(setOf("SUPER_ADMIN", "OWNER", "ADMIN", "MEMBER", "VIEWER"), seededNames)
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
                    ),
            )
        `when`(roleRepository.findByName(any())).thenReturn(Optional.empty())
        `when`(roleRepository.findByName("OWNER")).thenReturn(Optional.of(existingOwner))
        `when`(roleRepository.save(any<Role>())).thenAnswer { it.arguments[0] }

        roleSeeder.run(mock(ApplicationArguments::class.java))

        val captor = argumentCaptor<Role>()
        verify(roleRepository, times(4)).save(captor.capture())
        assertTrue(captor.allValues.none { it.name == "OWNER" })
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
        assertTrue(updatedMember.permissions.contains(Permissions.SESSION_READ))
        assertTrue(updatedMember.permissions.contains(Permissions.ORGANIZATION_READ))
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
                    ),
            )
        `when`(roleRepository.findByName(any())).thenReturn(Optional.empty())
        `when`(roleRepository.findByName("OWNER")).thenReturn(Optional.of(driftedRole))
        `when`(roleRepository.save(any<Role>())).thenAnswer { it.arguments[0] }

        roleSeeder.run(mock(ApplicationArguments::class.java))

        val captor = argumentCaptor<Role>()
        verify(roleRepository, times(5)).save(captor.capture())

        val updatedOwner = captor.allValues.first { it.name == "OWNER" }
        assertEquals("Organization owner with full org-level access", updatedOwner.description)
        assertEquals(RoleLevel.ORGANIZATION, updatedOwner.level)
        assertTrue(updatedOwner.isDefault)
    }

    @Test
    fun `should seed SUPER_ADMIN as system-level role with no permissions`() {
        `when`(roleRepository.findByName(any())).thenReturn(Optional.empty())
        `when`(roleRepository.save(any<Role>())).thenAnswer { it.arguments[0] }

        roleSeeder.run(mock(ApplicationArguments::class.java))

        val captor = argumentCaptor<Role>()
        verify(roleRepository, times(5)).save(captor.capture())

        val superAdmin = captor.allValues.first { it.name == "SUPER_ADMIN" }
        assertEquals(RoleLevel.SYSTEM, superAdmin.level)
        assertTrue(superAdmin.permissions.isEmpty())
    }

    @Test
    fun `should seed org-level roles with correct level`() {
        `when`(roleRepository.findByName(any())).thenReturn(Optional.empty())
        `when`(roleRepository.save(any<Role>())).thenAnswer { it.arguments[0] }

        roleSeeder.run(mock(ApplicationArguments::class.java))

        val captor = argumentCaptor<Role>()
        verify(roleRepository, times(5)).save(captor.capture())

        val orgRoles = captor.allValues.filter { it.name in setOf("OWNER", "ADMIN", "MEMBER", "VIEWER") }
        assertTrue(orgRoles.all { it.level == RoleLevel.ORGANIZATION })
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
                    ),
            )
        val viewer =
            Role(
                name = "VIEWER",
                description = "Read-only organization access",
                level = RoleLevel.ORGANIZATION,
                permissions = listOf(Permissions.SESSION_READ, Permissions.ORGANIZATION_READ),
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
