package com.aquinofroilan.tessera.aspect

import com.aquinofroilan.tessera.annotation.RequiresFeature
import com.aquinofroilan.tessera.domain.auth.model.RoleAssignment
import com.aquinofroilan.tessera.domain.auth.model.User
import com.aquinofroilan.tessera.domain.organization.model.FeatureFlag
import com.aquinofroilan.tessera.domain.organization.service.OrganizationBillingFeatureService
import com.aquinofroilan.tessera.exception.FeatureNotEnabledException
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.security.SessionContext
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import java.util.UUID

class FeatureFlagAspectTest {
    private lateinit var billingFeatureService: OrganizationBillingFeatureService
    private lateinit var authContext: AuthenticationContext
    private lateinit var aspect: FeatureFlagAspect

    private val orgId = UUID.fromString("4abe9f6d-6df3-6e5c-953e-3695db9a5216")
    private val userId = UUID.fromString("bc17c97c-3d89-7d43-b7e0-7ca0266eafa8")

    @BeforeEach
    fun setUp() {
        billingFeatureService = mock(OrganizationBillingFeatureService::class.java)
        authContext = mock(AuthenticationContext::class.java)
        aspect = FeatureFlagAspect(billingFeatureService, authContext)

        val testUser =
            User(
                uuid = userId,
                username = "testuser",
                email = "user@example.com",
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
                listOf(SimpleGrantedAuthority("ROLE_OWNER")),
            )
        authentication.details = SessionContext(UUID.randomUUID(), orgId)
        SecurityContextHolder.getContext().authentication = authentication

        `when`(authContext.organizationId()).thenReturn(orgId)
    }

    @RequiresFeature(FeatureFlag.ADVANCED_ANALYTICS)
    private fun sampleAnnotatedMethod() {}

    @RequiresFeature(key = "BETA_FEATURE")
    private fun sampleCustomKeyMethod() {}

    @Test
    fun `checkFeature succeeds when feature is enabled`() {
        `when`(billingFeatureService.isFeatureEnabled(orgId, FeatureFlag.ADVANCED_ANALYTICS.name)).thenReturn(true)

        val annotation = this::class.java.getDeclaredMethod("sampleAnnotatedMethod").getAnnotation(RequiresFeature::class.java)
        assertDoesNotThrow {
            aspect.checkFeature(annotation)
        }
    }

    @Test
    fun `checkFeature throws FeatureNotEnabledException when feature is disabled`() {
        `when`(billingFeatureService.isFeatureEnabled(orgId, FeatureFlag.ADVANCED_ANALYTICS.name)).thenReturn(false)

        val annotation = this::class.java.getDeclaredMethod("sampleAnnotatedMethod").getAnnotation(RequiresFeature::class.java)
        val ex =
            assertThrows<FeatureNotEnabledException> {
                aspect.checkFeature(annotation)
            }
        assertTrue(ex.message!!.contains("ADVANCED_ANALYTICS"))
    }

    @Test
    fun `checkFeature supports custom key string`() {
        `when`(billingFeatureService.isFeatureEnabled(orgId, "BETA_FEATURE")).thenReturn(true)

        val annotation = this::class.java.getDeclaredMethod("sampleCustomKeyMethod").getAnnotation(RequiresFeature::class.java)
        assertDoesNotThrow {
            aspect.checkFeature(annotation)
        }
    }

    @Test
    fun `checkFeature allows SUPER_ADMIN even if feature is disabled`() {
        val superAdmin =
            User(
                uuid = userId,
                username = "admin",
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

        `when`(billingFeatureService.isFeatureEnabled(orgId, FeatureFlag.ADVANCED_ANALYTICS.name)).thenReturn(false)

        val annotation = this::class.java.getDeclaredMethod("sampleAnnotatedMethod").getAnnotation(RequiresFeature::class.java)
        assertDoesNotThrow {
            aspect.checkFeature(annotation)
        }
    }
}
