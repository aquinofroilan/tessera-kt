package com.aquinofroilan.tessera.security

import com.aquinofroilan.tessera.model.User
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
        auth.details = SessionContext(sessionId = java.util.UUID.fromString("40b076e7-51d5-3d27-b9de-9e4d4e428928"), organizationId = java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"))
        SecurityContextHolder.getContext().authentication = auth

        assertThat(authContext.organizationId()).isEqualTo(java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"))
    }

    @Test
    fun `organizationId should return id from ApiKeyContext`() {
        val auth = UsernamePasswordAuthenticationToken("principal", null, emptyList())
        auth.details = ApiKeyContext(apiKeyId = java.util.UUID.fromString("498a5a34-2265-3162-959e-3ac30559a1b3"), organizationId = java.util.UUID.fromString("8576b8f7-dd04-3e57-b849-081b3776f223"))
        SecurityContextHolder.getContext().authentication = auth

        assertThat(authContext.organizationId()).isEqualTo(java.util.UUID.fromString("8576b8f7-dd04-3e57-b849-081b3776f223"))
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
                uuid = java.util.UUID.fromString("95401278-e202-3552-be90-1bbdf739eded"),
                username = "testuser",
                email = "test@example.com",
                firstName = "Test",
                lastName = "User",
                passwordHash = "encoded",
                organizationId = java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"),
            )
        val auth = UsernamePasswordAuthenticationToken(user, null, emptyList())
        SecurityContextHolder.getContext().authentication = auth

        assertThat(authContext.userId()).isEqualTo(java.util.UUID.fromString("95401278-e202-3552-be90-1bbdf739eded"))
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
