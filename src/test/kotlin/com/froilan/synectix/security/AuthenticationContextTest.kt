package com.froilan.synectix.security

import com.froilan.synectix.model.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

class AuthenticationContextTest {
    private val authContext = AuthenticationContext()

    @AfterEach
    fun cleanup() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `organizationId should return id from SessionContext`() {
        val auth = UsernamePasswordAuthenticationToken("principal", null, emptyList())
        auth.details = SessionContext(sessionId = "s-1", organizationId = "org-123")
        SecurityContextHolder.getContext().authentication = auth

        assertThat(authContext.organizationId()).isEqualTo("org-123")
    }

    @Test
    fun `organizationId should return id from ApiKeyContext`() {
        val auth = UsernamePasswordAuthenticationToken("principal", null, emptyList())
        auth.details = ApiKeyContext(apiKeyId = "ak-1", organizationId = "org-456")
        SecurityContextHolder.getContext().authentication = auth

        assertThat(authContext.organizationId()).isEqualTo("org-456")
    }

    @Test
    fun `organizationId should return null when no authentication`() {
        SecurityContextHolder.clearContext()

        assertThat(authContext.organizationId()).isNull()
    }

    @Test
    fun `organizationId should return null when details is not SessionContext or ApiKeyContext`() {
        val auth = UsernamePasswordAuthenticationToken("principal", null, emptyList())
        auth.details = "some-string"
        SecurityContextHolder.getContext().authentication = auth

        assertThat(authContext.organizationId()).isNull()
    }

    @Test
    fun `organizationId should return null when details is null`() {
        val auth = UsernamePasswordAuthenticationToken("principal", null, emptyList())
        SecurityContextHolder.getContext().authentication = auth

        assertThat(authContext.organizationId()).isNull()
    }

    @Test
    fun `userId should return uuid from User principal`() {
        val user =
            User(
                uuid = "user-789",
                username = "testuser",
                email = "test@example.com",
                firstName = "Test",
                lastName = "User",
                passwordHash = "encoded",
                organizationId = "org-123",
            )
        val auth = UsernamePasswordAuthenticationToken(user, null, emptyList())
        SecurityContextHolder.getContext().authentication = auth

        assertThat(authContext.userId()).isEqualTo("user-789")
    }

    @Test
    fun `userId should return null when principal is not User`() {
        val auth = UsernamePasswordAuthenticationToken("string-principal", null, emptyList())
        SecurityContextHolder.getContext().authentication = auth

        assertThat(authContext.userId()).isNull()
    }

    @Test
    fun `userId should return null when no authentication`() {
        SecurityContextHolder.clearContext()

        assertThat(authContext.userId()).isNull()
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `unauthorized should return 401 with error message`() {
        val response = authContext.unauthorized()

        assertThat(response.statusCode.value()).isEqualTo(401)
        val body = response.body as Map<String, String>
        assertThat(body["error"]).isEqualTo("Authentication required")
    }
}
