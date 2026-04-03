package com.froilan.synectix.security

import com.froilan.synectix.model.Role
import com.froilan.synectix.model.RoleLevel
import com.froilan.synectix.repository.RoleRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RolePermissionCacheTest {
    private lateinit var roleRepository: RoleRepository
    private lateinit var cache: RolePermissionCache

    @BeforeEach
    fun setup() {
        roleRepository = mock(RoleRepository::class.java)

        val roles =
            listOf(
                Role(
                    name = "OWNER",
                    description = "Owner",
                    level = RoleLevel.ORGANIZATION,
                    permissions = listOf("session:read", "session:delete", "user:read"),
                ),
                Role(
                    name = "VIEWER",
                    description = "Viewer",
                    level = RoleLevel.ORGANIZATION,
                    permissions = listOf("session:read"),
                ),
            )

        `when`(roleRepository.findAll()).thenReturn(roles)
        `when`(roleRepository.findByName("OWNER")).thenReturn(Optional.of(roles[0]))
        `when`(roleRepository.findByName("VIEWER")).thenReturn(Optional.of(roles[1]))
        `when`(roleRepository.findByName("UNKNOWN")).thenReturn(Optional.empty())

        cache = RolePermissionCache(roleRepository)
        cache.loadAll()
    }

    @Test
    fun `should return permissions for known role`() {
        val permissions = cache.getPermissions("OWNER")
        assertEquals(setOf("session:read", "session:delete", "user:read"), permissions)
    }

    @Test
    fun `should return empty set for unknown role`() {
        val permissions = cache.getPermissions("UNKNOWN")
        assertTrue(permissions.isEmpty())
    }

    @Test
    fun `refresh should reload from repository`() {
        val updatedRole =
            Role(
                name = "VIEWER",
                description = "Viewer",
                level = RoleLevel.ORGANIZATION,
                permissions = listOf("session:read", "organization:read"),
            )
        `when`(roleRepository.findAll()).thenReturn(listOf(updatedRole))

        cache.refresh()

        assertEquals(setOf("session:read", "organization:read"), cache.getPermissions("VIEWER"))
    }
}
