package com.aquinofroilan.tessera.config

import com.aquinofroilan.tessera.domain.auth.model.RoleAssignment
import com.aquinofroilan.tessera.domain.auth.model.User
import com.aquinofroilan.tessera.domain.organization.model.OrganizationStatus
import com.aquinofroilan.tessera.domain.organization.service.OrganizationLifecycleService
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.security.SessionContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import java.util.UUID

class OrganizationStatusInterceptorTest {
    private lateinit var lifecycleService: OrganizationLifecycleService
    private lateinit var authContext: AuthenticationContext
    private lateinit var interceptor: OrganizationStatusInterceptor

    private val orgId = UUID.fromString("4abe9f6d-6df3-6e5c-953e-3695db9a5216")
    private val userId = UUID.fromString("bc17c97c-3d89-7d43-b7e0-7ca0266eafa8")

    @BeforeEach
    fun setUp() {
        lifecycleService = mock(OrganizationLifecycleService::class.java)
        authContext = mock(AuthenticationContext::class.java)
        interceptor = OrganizationStatusInterceptor(authContext, java.util.Optional.of(lifecycleService))

        val testUser =
            User(
                uuid = userId,
                username = "testuser",
                email = "test@example.com",
                firstName = "Test",
                lastName = "User",
                passwordHash = "encoded",
                organizationId = orgId,
                roleAssignments = listOf(RoleAssignment("OWNER", orgId)),
            )
        val authentication =
            UsernamePasswordAuthenticationToken(
                testUser,
                null,
                listOf(SimpleGrantedAuthority("ROLE_OWNER"), SimpleGrantedAuthority("organization:read")),
            )
        authentication.details = SessionContext(UUID.randomUUID(), orgId)
        SecurityContextHolder.getContext().authentication = authentication

        `when`(authContext.organizationId()).thenReturn(orgId)
    }

    @Test
    fun `preHandle allows exempt paths without checking org status`() {
        val request = MockHttpServletRequest("GET", "/api/v1/auth/me")
        val response = MockHttpServletResponse()

        assertTrue(interceptor.preHandle(request, response, Any()))
    }

    @Test
    fun `preHandle allows status endpoint path for suspended org to allow reactivation`() {
        val request = MockHttpServletRequest("POST", "/api/v1/organization/status/activate")
        val response = MockHttpServletResponse()

        `when`(lifecycleService.getCachedStatus(orgId)).thenReturn(OrganizationStatus.SUSPENDED)

        assertTrue(interceptor.preHandle(request, response, Any()))
    }

    @Test
    fun `preHandle allows request when org is ACTIVE`() {
        val request = MockHttpServletRequest("POST", "/api/v1/inventory/products")
        val response = MockHttpServletResponse()

        `when`(lifecycleService.getCachedStatus(orgId)).thenReturn(OrganizationStatus.ACTIVE)

        assertTrue(interceptor.preHandle(request, response, Any()))
    }

    @Test
    fun `preHandle blocks request with 403 when org is SUSPENDED`() {
        val request = MockHttpServletRequest("GET", "/api/v1/inventory/products")
        val response = MockHttpServletResponse()

        `when`(lifecycleService.getCachedStatus(orgId)).thenReturn(OrganizationStatus.SUSPENDED)

        val result = interceptor.preHandle(request, response, Any())

        assertFalse(result)
        assertEquals(403, response.status)
        assertTrue(response.contentAsString.contains("Organization is suspended"))
    }

    @Test
    fun `preHandle allows read GET request when org is ARCHIVED`() {
        val request = MockHttpServletRequest("GET", "/api/v1/finance/reports/balance-sheet")
        val response = MockHttpServletResponse()

        `when`(lifecycleService.getCachedStatus(orgId)).thenReturn(OrganizationStatus.ARCHIVED)

        assertTrue(interceptor.preHandle(request, response, Any()))
    }

    @Test
    fun `preHandle blocks mutating POST request with 403 when org is ARCHIVED`() {
        val request = MockHttpServletRequest("POST", "/api/v1/finance/accounts")
        val response = MockHttpServletResponse()

        `when`(lifecycleService.getCachedStatus(orgId)).thenReturn(OrganizationStatus.ARCHIVED)

        val result = interceptor.preHandle(request, response, Any())

        assertFalse(result)
        assertEquals(403, response.status)
        assertTrue(response.contentAsString.contains("Organization is archived and read-only"))
    }

    @Test
    fun `preHandle allows SUPER_ADMIN even when org is SUSPENDED`() {
        val superAdmin =
            User(
                uuid = userId,
                username = "superadmin",
                email = "admin@example.com",
                firstName = "Super",
                lastName = "Admin",
                passwordHash = "encoded",
                organizationId = orgId,
                roleAssignments = listOf(RoleAssignment("SUPER_ADMIN", orgId)),
            )
        val authentication =
            UsernamePasswordAuthenticationToken(
                superAdmin,
                null,
                listOf(SimpleGrantedAuthority("ROLE_SUPER_ADMIN")),
            )
        SecurityContextHolder.getContext().authentication = authentication

        val request = MockHttpServletRequest("POST", "/api/v1/inventory/products")
        val response = MockHttpServletResponse()

        `when`(lifecycleService.getCachedStatus(orgId)).thenReturn(OrganizationStatus.SUSPENDED)

        assertTrue(interceptor.preHandle(request, response, Any()))
    }
}
