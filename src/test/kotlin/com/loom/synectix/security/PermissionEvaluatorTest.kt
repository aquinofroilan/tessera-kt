package com.loom.synectix.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority

class PermissionEvaluatorTest {
    private lateinit var evaluator: SynectixPermissionEvaluator

    @BeforeEach
    fun setup() {
        evaluator = SynectixPermissionEvaluator()
    }

    @Test
    fun `SUPER_ADMIN should bypass any permission check`() {
        val auth = authWith("ROLE_SUPER_ADMIN")

        assertThat(evaluator.hasPermission(auth, Unit, "session:read")).isTrue()
        assertThat(evaluator.hasPermission(auth, Unit, "anything:whatever")).isTrue()
    }

    @Test
    fun `user with matching authority should pass`() {
        val auth = authWith("ROLE_MEMBER", "session:read", "organization:read")

        assertThat(evaluator.hasPermission(auth, Unit, "session:read")).isTrue()
        assertThat(evaluator.hasPermission(auth, Unit, "organization:read")).isTrue()
    }

    @Test
    fun `user without matching authority should fail`() {
        val auth = authWith("ROLE_MEMBER", "session:read")

        assertThat(evaluator.hasPermission(auth, Unit, "session:delete")).isFalse()
        assertThat(evaluator.hasPermission(auth, Unit, "user:write")).isFalse()
    }

    @Test
    fun `user with no authorities should fail`() {
        val auth = authWith()

        assertThat(evaluator.hasPermission(auth, Unit, "session:read")).isFalse()
    }

    @Test
    fun `non-string permission should return false`() {
        val auth = authWith("ROLE_ADMIN", "session:read")

        assertThat(evaluator.hasPermission(auth, Unit, 123)).isFalse()
    }

    @Test
    fun `targetId overload should delegate to primary`() {
        val auth = authWith("ROLE_SUPER_ADMIN")

        assertThat(evaluator.hasPermission(auth, "id-1", "Session", "session:read")).isTrue()
    }

    private fun authWith(vararg authorities: String) =
        UsernamePasswordAuthenticationToken(
            "user",
            null,
            authorities.map { SimpleGrantedAuthority(it) },
        )
}
