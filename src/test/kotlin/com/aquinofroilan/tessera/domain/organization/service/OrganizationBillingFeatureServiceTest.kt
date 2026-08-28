package com.aquinofroilan.tessera.domain.organization.service

import com.aquinofroilan.tessera.domain.organization.dto.BatchSetFeatureOverridesRequest
import com.aquinofroilan.tessera.domain.organization.model.BillingPlan
import com.aquinofroilan.tessera.domain.organization.model.FeatureFlag
import com.aquinofroilan.tessera.domain.organization.model.InventoryCostingMethod
import com.aquinofroilan.tessera.domain.organization.model.OrganizationFeatureFlag
import com.aquinofroilan.tessera.domain.organization.model.OrganizationStatus
import com.aquinofroilan.tessera.domain.organization.model.Organizations
import com.aquinofroilan.tessera.domain.organization.repository.OrganizationFeatureFlagRepository
import com.aquinofroilan.tessera.domain.organization.repository.OrganizationRepository
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

class OrganizationBillingFeatureServiceTest {
    private lateinit var organizationRepository: OrganizationRepository
    private lateinit var featureFlagRepository: OrganizationFeatureFlagRepository
    private lateinit var service: OrganizationBillingFeatureService

    private val orgId = UUID.fromString("4abe9f6d-6df3-6e5c-953e-3695db9a5216")

    @BeforeEach
    fun setUp() {
        organizationRepository = mock(OrganizationRepository::class.java)
        featureFlagRepository = mock(OrganizationFeatureFlagRepository::class.java)
        service = OrganizationBillingFeatureService(organizationRepository, featureFlagRepository)
    }

    private fun createOrg(plan: BillingPlan = BillingPlan.FREE): Organizations =
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
            status = OrganizationStatus.ACTIVE,
            billingPlan = plan,
            inventoryCostingMethod = InventoryCostingMethod.WEIGHTED_AVERAGE,
            inventoryGlPostingEnabled = false,
            isActive = true,
        )

    @Test
    fun `getPlan returns FREE plan with default flags`() {
        val org = createOrg(BillingPlan.FREE)
        `when`(organizationRepository.findById(orgId)).thenReturn(Optional.of(org))
        `when`(featureFlagRepository.findByOrganizationId(orgId)).thenReturn(emptyList())

        val response = service.getPlan(orgId)

        assertEquals(BillingPlan.FREE, response.billingPlan)
        assertFalse(response.effectiveFeatures[FeatureFlag.API_KEYS.name] ?: true)
        assertFalse(response.effectiveFeatures[FeatureFlag.ADVANCED_ANALYTICS.name] ?: true)
        assertTrue(response.featureOverrides.isEmpty())
    }

    @Test
    fun `getPlan returns STARTER plan with enabled starter features`() {
        val org = createOrg(BillingPlan.STARTER)
        `when`(organizationRepository.findById(orgId)).thenReturn(Optional.of(org))
        `when`(featureFlagRepository.findByOrganizationId(orgId)).thenReturn(emptyList())

        val response = service.getPlan(orgId)

        assertEquals(BillingPlan.STARTER, response.billingPlan)
        assertTrue(response.effectiveFeatures[FeatureFlag.API_KEYS.name] == true)
        assertTrue(response.effectiveFeatures[FeatureFlag.MULTI_CURRENCY.name] == true)
        assertTrue(response.effectiveFeatures[FeatureFlag.CUSTOM_ROLES.name] == true)
        assertFalse(response.effectiveFeatures[FeatureFlag.ADVANCED_ANALYTICS.name] ?: true)
    }

    @Test
    fun `getPlan returns ENTERPRISE plan with all features enabled`() {
        val org = createOrg(BillingPlan.ENTERPRISE)
        `when`(organizationRepository.findById(orgId)).thenReturn(Optional.of(org))
        `when`(featureFlagRepository.findByOrganizationId(orgId)).thenReturn(emptyList())

        val response = service.getPlan(orgId)

        assertEquals(BillingPlan.ENTERPRISE, response.billingPlan)
        assertTrue(response.effectiveFeatures[FeatureFlag.API_KEYS.name] == true)
        assertTrue(response.effectiveFeatures[FeatureFlag.ADVANCED_ANALYTICS.name] == true)
        assertTrue(response.effectiveFeatures[FeatureFlag.MFG_MODULE.name] == true)
        assertTrue(response.effectiveFeatures[FeatureFlag.WORKFLOW_AUTOMATION.name] == true)
    }

    @Test
    fun `getPlan applies feature flag overrides over plan defaults`() {
        val org = createOrg(BillingPlan.FREE)
        `when`(organizationRepository.findById(orgId)).thenReturn(Optional.of(org))
        `when`(featureFlagRepository.findByOrganizationId(orgId)).thenReturn(
            listOf(
                OrganizationFeatureFlag(
                    organizationId = orgId,
                    featureKey = FeatureFlag.API_KEYS.name,
                    enabled = true,
                ),
            ),
        )

        val response = service.getPlan(orgId)

        assertEquals(BillingPlan.FREE, response.billingPlan)
        assertTrue(response.effectiveFeatures[FeatureFlag.API_KEYS.name] == true)
        assertFalse(response.effectiveFeatures[FeatureFlag.ADVANCED_ANALYTICS.name] ?: true)
        assertEquals(true, response.featureOverrides[FeatureFlag.API_KEYS.name])
    }

    @Test
    fun `updatePlan updates organization plan tier`() {
        val org = createOrg(BillingPlan.FREE)
        `when`(organizationRepository.findById(orgId)).thenReturn(Optional.of(org))
        `when`(organizationRepository.save(any<Organizations>())).thenAnswer { it.arguments[0] }
        `when`(featureFlagRepository.findByOrganizationId(orgId)).thenReturn(emptyList())

        val response = service.updatePlan(orgId, BillingPlan.ENTERPRISE)

        assertEquals(BillingPlan.ENTERPRISE, response.billingPlan)
        assertEquals(BillingPlan.ENTERPRISE, org.billingPlan)
    }

    @Test
    fun `isFeatureEnabled evaluates plan defaults and overrides correctly`() {
        val org = createOrg(BillingPlan.FREE)
        `when`(organizationRepository.findById(orgId)).thenReturn(Optional.of(org))
        `when`(featureFlagRepository.findByOrganizationId(orgId)).thenReturn(
            listOf(
                OrganizationFeatureFlag(
                    organizationId = orgId,
                    featureKey = FeatureFlag.API_KEYS.name,
                    enabled = true,
                ),
            ),
        )

        assertTrue(service.isFeatureEnabled(orgId, FeatureFlag.API_KEYS.name))
        assertFalse(service.isFeatureEnabled(orgId, FeatureFlag.ADVANCED_ANALYTICS.name))
    }

    @Test
    fun `setFeatureOverride creates and saves override`() {
        val org = createOrg(BillingPlan.FREE)
        `when`(organizationRepository.findById(orgId)).thenReturn(Optional.of(org))
        `when`(
            featureFlagRepository.findByOrganizationIdAndFeatureKey(
                orgId,
                FeatureFlag.ADVANCED_ANALYTICS.name,
            ),
        ).thenReturn(Optional.empty())
        `when`(featureFlagRepository.save(any<OrganizationFeatureFlag>())).thenAnswer { it.arguments[0] }

        val detail = service.setFeatureOverride(orgId, FeatureFlag.ADVANCED_ANALYTICS.name, true)

        assertEquals(FeatureFlag.ADVANCED_ANALYTICS.name, detail.featureKey)
        assertTrue(detail.enabled)
        assertTrue(detail.isOverride)
        assertFalse(detail.planDefault)
    }

    @Test
    fun `removeFeatureOverride deletes override and reverts to plan default`() {
        val org = createOrg(BillingPlan.STARTER)
        `when`(organizationRepository.findById(orgId)).thenReturn(Optional.of(org))

        val detail = service.removeFeatureOverride(orgId, FeatureFlag.API_KEYS.name)

        assertEquals(FeatureFlag.API_KEYS.name, detail.featureKey)
        assertTrue(detail.enabled) // Starter has API_KEYS enabled by default
        assertFalse(detail.isOverride)
        assertTrue(detail.planDefault)
    }

    @Test
    fun `batchSetOverrides saves multiple overrides`() {
        val org = createOrg(BillingPlan.FREE)
        `when`(organizationRepository.findById(orgId)).thenReturn(Optional.of(org))
        `when`(featureFlagRepository.findByOrganizationId(orgId)).thenReturn(emptyList())

        val request =
            BatchSetFeatureOverridesRequest(
                overrides =
                    mapOf(
                        FeatureFlag.API_KEYS.name to true,
                        FeatureFlag.MFG_MODULE.name to true,
                    ),
            )

        val response = service.batchSetOverrides(orgId, request)

        assertEquals(BillingPlan.FREE, response.billingPlan)
    }

    @Test
    fun `getPlan throws ResourceNotFoundException when org does not exist`() {
        `when`(organizationRepository.findById(orgId)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            service.getPlan(orgId)
        }
    }
}
