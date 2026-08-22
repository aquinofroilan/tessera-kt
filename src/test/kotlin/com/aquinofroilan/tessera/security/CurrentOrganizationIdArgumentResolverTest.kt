package com.aquinofroilan.tessera.security

import com.aquinofroilan.tessera.exception.AuthenticationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.`when`
import org.mockito.kotlin.mock
import org.springframework.core.MethodParameter
import java.util.UUID

class CurrentOrganizationIdArgumentResolverTest {
    private val authContext: AuthenticationContext = mock()
    private val resolver = CurrentOrganizationIdArgumentResolver(authContext)

    @Test
    fun `supportsParameter returns true when CurrentOrganizationId annotation present`() {
        val param: MethodParameter = mock()
        `when`(param.hasParameterAnnotation(CurrentOrganizationId::class.java)).thenReturn(true)

        assertTrue(resolver.supportsParameter(param))
    }

    @Test
    fun `supportsParameter returns false when annotation absent`() {
        val param: MethodParameter = mock()
        `when`(param.hasParameterAnnotation(CurrentOrganizationId::class.java)).thenReturn(false)

        assertFalse(resolver.supportsParameter(param))
    }

    @Test
    fun `resolveArgument returns orgId when authenticated`() {
        val expectedOrgId = UUID.randomUUID()
        `when`(authContext.organizationId()).thenReturn(expectedOrgId)

        val param: MethodParameter = mock()
        val result = resolver.resolveArgument(param, null, mock(), null)

        assertEquals(expectedOrgId, result)
    }

    @Test
    fun `resolveArgument throws AuthenticationException when unauthenticated`() {
        `when`(authContext.organizationId()).thenReturn(null)

        val param: MethodParameter = mock()
        assertThrows<AuthenticationException> {
            resolver.resolveArgument(param, null, mock(), null)
        }
    }
}
