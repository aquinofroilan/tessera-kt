package com.aquinofroilan.tessera.domain.organization.service

import com.aquinofroilan.tessera.domain.organization.model.InventoryCostingMethod
import com.aquinofroilan.tessera.domain.organization.model.OrganizationStatus
import com.aquinofroilan.tessera.domain.organization.model.Organizations
import com.aquinofroilan.tessera.domain.organization.repository.OrganizationRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

class OrganizationLifecycleServiceTest {
    private lateinit var organizationRepository: OrganizationRepository
    private lateinit var service: OrganizationLifecycleService

    private val orgId = UUID.fromString("4abe9f6d-6df3-6e5c-953e-3695db9a5216")

    @BeforeEach
    fun setUp() {
        organizationRepository = mock(OrganizationRepository::class.java)
        service = OrganizationLifecycleService(organizationRepository)
    }

    private fun createOrg(status: OrganizationStatus = OrganizationStatus.ACTIVE): Organizations =
        Organizations(
            uuid = orgId,
            orgSlug = "acme-corp",
            name = "Acme Corp",
            description = "Description",
            legalName = "Acme Inc.",
            tradeName = "Acme",
            baseCurrency = "USD",
            fiscalYearStart = LocalDateTime.of(2026, 1, 1, 0, 0),
            timezone = "UTC",
            logoUrl = "https://example.com/logo.png",
            status = status,
            inventoryCostingMethod = InventoryCostingMethod.WEIGHTED_AVERAGE,
            inventoryGlPostingEnabled = false,
            isActive = (status == OrganizationStatus.ACTIVE),
        )

    @Test
    fun `getStatus returns ACTIVE status with allowed transitions`() {
        val org = createOrg(OrganizationStatus.ACTIVE)
        `when`(organizationRepository.findById(orgId)).thenReturn(Optional.of(org))

        val response = service.getStatus(orgId)

        assertEquals(OrganizationStatus.ACTIVE, response.status)
        assertFalse(response.accessBlocked)
        assertFalse(response.readOnly)
        assertEquals(
            listOf(OrganizationStatus.SUSPENDED, OrganizationStatus.ARCHIVED),
            response.allowedTransitions,
        )
    }

    @Test
    fun `getStatus returns SUSPENDED status and flags access blocked`() {
        val org = createOrg(OrganizationStatus.SUSPENDED)
        `when`(organizationRepository.findById(orgId)).thenReturn(Optional.of(org))

        val response = service.getStatus(orgId)

        assertEquals(OrganizationStatus.SUSPENDED, response.status)
        assertTrue(response.accessBlocked)
        assertFalse(response.readOnly)
        assertEquals(
            listOf(OrganizationStatus.ACTIVE, OrganizationStatus.ARCHIVED),
            response.allowedTransitions,
        )
    }

    @Test
    fun `getStatus returns ARCHIVED status and flags read-only`() {
        val org = createOrg(OrganizationStatus.ARCHIVED)
        `when`(organizationRepository.findById(orgId)).thenReturn(Optional.of(org))

        val response = service.getStatus(orgId)

        assertEquals(OrganizationStatus.ARCHIVED, response.status)
        assertFalse(response.accessBlocked)
        assertTrue(response.readOnly)
        assertEquals(
            listOf(OrganizationStatus.ACTIVE, OrganizationStatus.SUSPENDED),
            response.allowedTransitions,
        )
    }

    @Test
    fun `transitionStatus from ACTIVE to SUSPENDED sets status and deactivates isActive`() {
        val org = createOrg(OrganizationStatus.ACTIVE)
        `when`(organizationRepository.findById(orgId)).thenReturn(Optional.of(org))
        `when`(organizationRepository.save(any<Organizations>())).thenAnswer { it.arguments[0] }

        val response = service.transitionStatus(orgId, OrganizationStatus.SUSPENDED, "Administrative Hold")

        assertEquals(OrganizationStatus.SUSPENDED, response.status)
        assertTrue(response.accessBlocked)
        assertFalse(org.isActive)
    }

    @Test
    fun `transitionStatus from SUSPENDED to ACTIVE sets status and reactivates isActive`() {
        val org = createOrg(OrganizationStatus.SUSPENDED)
        `when`(organizationRepository.findById(orgId)).thenReturn(Optional.of(org))
        `when`(organizationRepository.save(any<Organizations>())).thenAnswer { it.arguments[0] }

        val response = service.transitionStatus(orgId, OrganizationStatus.ACTIVE, "Reactivation")

        assertEquals(OrganizationStatus.ACTIVE, response.status)
        assertFalse(response.accessBlocked)
        assertTrue(org.isActive)
    }

    @Test
    fun `transitionStatus from ACTIVE to ARCHIVED sets status and marks read-only`() {
        val org = createOrg(OrganizationStatus.ACTIVE)
        `when`(organizationRepository.findById(orgId)).thenReturn(Optional.of(org))
        `when`(organizationRepository.save(any<Organizations>())).thenAnswer { it.arguments[0] }

        val response = service.transitionStatus(orgId, OrganizationStatus.ARCHIVED, "Tenant archived")

        assertEquals(OrganizationStatus.ARCHIVED, response.status)
        assertTrue(response.readOnly)
    }

    @Test
    fun `transitionStatus from ARCHIVED to ACTIVE restores active status`() {
        val org = createOrg(OrganizationStatus.ARCHIVED)
        `when`(organizationRepository.findById(orgId)).thenReturn(Optional.of(org))
        `when`(organizationRepository.save(any<Organizations>())).thenAnswer { it.arguments[0] }

        val response = service.transitionStatus(orgId, OrganizationStatus.ACTIVE, "Unarchived")

        assertEquals(OrganizationStatus.ACTIVE, response.status)
        assertFalse(response.readOnly)
        assertTrue(org.isActive)
    }

    @Test
    fun `transitionStatus throws BusinessRuleException when target status is identical`() {
        val org = createOrg(OrganizationStatus.ACTIVE)
        `when`(organizationRepository.findById(orgId)).thenReturn(Optional.of(org))

        val ex =
            assertThrows<BusinessRuleException> {
                service.transitionStatus(orgId, OrganizationStatus.ACTIVE)
            }
        assertTrue(ex.message!!.contains("already in status ACTIVE"))
    }

    @Test
    fun `transitionStatus throws ResourceNotFoundException when org does not exist`() {
        `when`(organizationRepository.findById(orgId)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            service.transitionStatus(orgId, OrganizationStatus.SUSPENDED)
        }
    }
}
