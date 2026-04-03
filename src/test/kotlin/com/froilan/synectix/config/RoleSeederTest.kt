package com.froilan.synectix.config

import com.froilan.synectix.model.Role
import com.froilan.synectix.model.RoleLevel
import com.froilan.synectix.repository.RoleRepository
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
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoleSeederTest {
    private lateinit var roleRepository: RoleRepository
    private lateinit var roleSeeder: RoleSeeder

    @BeforeEach
    fun setup() {
        roleRepository = mock(RoleRepository::class.java)
        roleSeeder = RoleSeeder(roleRepository)
    }

    @Test
    fun `should seed all default roles when none exist`() {
        `when`(roleRepository.existsByName(any())).thenReturn(false)
        `when`(roleRepository.save(any<Role>())).thenAnswer { it.arguments[0] }

        roleSeeder.run(mock(ApplicationArguments::class.java))

        val captor = argumentCaptor<Role>()
        verify(roleRepository, times(5)).save(captor.capture())

        val seededNames = captor.allValues.map { it.name }.toSet()
        assertEquals(setOf("SUPER_ADMIN", "OWNER", "ADMIN", "MEMBER", "VIEWER"), seededNames)
    }

    @Test
    fun `should not duplicate roles when they already exist`() {
        `when`(roleRepository.existsByName(any())).thenReturn(true)

        roleSeeder.run(mock(ApplicationArguments::class.java))

        verify(roleRepository, never()).save(any<Role>())
    }

    @Test
    fun `should seed SUPER_ADMIN as system-level role`() {
        `when`(roleRepository.existsByName(any())).thenReturn(false)
        `when`(roleRepository.save(any<Role>())).thenAnswer { it.arguments[0] }

        roleSeeder.run(mock(ApplicationArguments::class.java))

        val captor = argumentCaptor<Role>()
        verify(roleRepository, times(5)).save(captor.capture())

        val superAdmin = captor.allValues.first { it.name == "SUPER_ADMIN" }
        assertEquals(RoleLevel.SYSTEM, superAdmin.level)
    }

    @Test
    fun `should seed org-level roles with correct level`() {
        `when`(roleRepository.existsByName(any())).thenReturn(false)
        `when`(roleRepository.save(any<Role>())).thenAnswer { it.arguments[0] }

        roleSeeder.run(mock(ApplicationArguments::class.java))

        val captor = argumentCaptor<Role>()
        verify(roleRepository, times(5)).save(captor.capture())

        val orgRoles = captor.allValues.filter { it.name in setOf("OWNER", "ADMIN", "MEMBER", "VIEWER") }
        assertTrue(orgRoles.all { it.level == RoleLevel.ORGANIZATION })
    }

    @Test
    fun `should mark OWNER as default role`() {
        `when`(roleRepository.existsByName(any())).thenReturn(false)
        `when`(roleRepository.save(any<Role>())).thenAnswer { it.arguments[0] }

        roleSeeder.run(mock(ApplicationArguments::class.java))

        val captor = argumentCaptor<Role>()
        verify(roleRepository, times(5)).save(captor.capture())

        val owner = captor.allValues.first { it.name == "OWNER" }
        assertTrue(owner.isDefault)

        val nonDefaults = captor.allValues.filter { it.name != "OWNER" }
        assertTrue(nonDefaults.none { it.isDefault })
    }
}
