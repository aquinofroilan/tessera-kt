package com.froilan.synectix.security

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PermissionEvaluatorTest {
    private lateinit var evaluator: SynectixPermissionEvaluator

    @BeforeEach
    fun setup() {
        evaluator = SynectixPermissionEvaluator()
    }

    @Test
    fun `SUPER_ADMIN should bypass any permission check`() {
        val auth = authWith("ROLE_SUPER_ADMIN")

        assertTrue(evaluator.hasPermission(auth, Unit, "session:read"))
        assertTrue(evaluator.hasPermission(auth, Unit, "anything:whatever"))
    }

    @Test
    fun `user with matching authority should pass`() {
        val auth = authWith("ROLE_MEMBER", "session:read", "organization:read")

        assertTrue(evaluator.hasPermission(auth, Unit, "session:read"))
        assertTrue(evaluator.hasPermission(auth, Unit, "organization:read"))
    }

    @Test
    fun `user without matching authority should fail`() {
        val auth = authWith("ROLE_MEMBER", "session:read")

        assertFalse(evaluator.hasPermission(auth, Unit, "session:delete"))
        assertFalse(evaluator.hasPermission(auth, Unit, "user:write"))
    }

    @Test
    fun `user with no authorities should fail`() {
        val auth = authWith()

        assertFalse(evaluator.hasPermission(auth, Unit, "session:read"))
    }

    @Test
    fun `non-string permission should return false`() {
        val auth = authWith("ROLE_ADMIN", "session:read")

        assertFalse(evaluator.hasPermission(auth, Unit, 123))
    }

    @Test
    fun `targetId overload should delegate to primary`() {
        val auth = authWith("ROLE_SUPER_ADMIN")

        assertTrue(evaluator.hasPermission(auth, "id-1", "Session", "session:read"))
    }

    private fun authWith(vararg authorities: String) =
        UsernamePasswordAuthenticationToken(
            "user",
            null,
            authorities.map { SimpleGrantedAuthority(it) },
        )
}
