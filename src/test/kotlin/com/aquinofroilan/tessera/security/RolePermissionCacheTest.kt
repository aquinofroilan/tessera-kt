package com.aquinofroilan.tessera.security

import com.aquinofroilan.tessera.domain.auth.model.Role
import com.aquinofroilan.tessera.domain.auth.model.RoleLevel
import com.aquinofroilan.tessera.domain.auth.repository.RoleRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.Optional

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
        assertThat(permissions).isEqualTo(setOf("session:read", "session:delete", "user:read"))
    }

    @Test
    fun `should return empty set for unknown role`() {
        val permissions = cache.getPermissions("UNKNOWN")
        assertThat(permissions).isEmpty()
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

        assertThat(cache.getPermissions("VIEWER")).isEqualTo(setOf("session:read", "organization:read"))
    }
}
