package com.aquinofroilan.tessera.domain.organization.service

import com.aquinofroilan.tessera.domain.finance.repository.CurrencyRepository
import com.aquinofroilan.tessera.domain.organization.dto.UpdateOrganizationSettingsRequest
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

class OrganizationSettingsServiceTest {
    private lateinit var organizationRepository: OrganizationRepository
    private lateinit var currencyRepository: CurrencyRepository
    private lateinit var service: OrganizationSettingsService

    private val orgId = UUID.fromString("4abe9f6d-6df3-6e5c-953e-3695db9a5216")

    @BeforeEach
    fun setUp() {
        organizationRepository = mock(OrganizationRepository::class.java)
        currencyRepository = mock(CurrencyRepository::class.java)
        service = OrganizationSettingsService(organizationRepository, currencyRepository)
    }

    private fun createOrg(): Organizations =
        Organizations(
            uuid = orgId,
            orgSlug = "acme-corp",
            name = "Acme Corp",
            description = "Original Description",
            legalName = "Acme Corporation Inc.",
            tradeName = "Acme",
            baseCurrency = "USD",
            fiscalYearStart = LocalDateTime.of(2026, 1, 1, 0, 0),
            timezone = "UTC",
            logoUrl = "https://example.com/logo.png",
            status = OrganizationStatus.ACTIVE,
            inventoryCostingMethod = InventoryCostingMethod.WEIGHTED_AVERAGE,
            inventoryGlPostingEnabled = false,
        )

    @Test
    fun `getSettings returns settings when org exists`() {
        val org = createOrg()
        `when`(organizationRepository.findById(orgId)).thenReturn(Optional.of(org))

        val response = service.getSettings(orgId)

        assertEquals(orgId, response.id)
        assertEquals("acme-corp", response.orgSlug)
        assertEquals("Acme Corp", response.name)
        assertEquals("USD", response.baseCurrency)
        assertEquals("UTC", response.timezone)
        assertEquals("https://example.com/logo.png", response.logoUrl)
        assertEquals(InventoryCostingMethod.WEIGHTED_AVERAGE, response.inventoryCostingMethod)
        assertFalse(response.inventoryGlPostingEnabled)
    }

    @Test
    fun `getSettings throws ResourceNotFoundException when org does not exist`() {
        `when`(organizationRepository.findById(orgId)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            service.getSettings(orgId)
        }
    }

    @Test
    fun `updateSettings updates all fields successfully`() {
        val org = createOrg()
        `when`(organizationRepository.findById(orgId)).thenReturn(Optional.of(org))
        `when`(currencyRepository.existsById("EUR")).thenReturn(true)
        `when`(organizationRepository.existsByName("Acme Global")).thenReturn(false)
        `when`(organizationRepository.save(any<Organizations>())).thenAnswer { it.arguments[0] }

        val newFiscalStart = LocalDateTime.of(2026, 7, 1, 0, 0)
        val request =
            UpdateOrganizationSettingsRequest(
                name = "Acme Global",
                description = "New Description",
                legalName = "Acme Global International LLC",
                tradeName = "Acme Global",
                baseCurrency = "EUR",
                fiscalYearStart = newFiscalStart,
                timezone = "America/New_York",
                logoUrl = "https://example.com/new-logo.svg",
                inventoryCostingMethod = InventoryCostingMethod.FIFO,
                inventoryGlPostingEnabled = true,
            )

        val response = service.updateSettings(orgId, request)

        assertEquals("Acme Global", response.name)
        assertEquals("New Description", response.description)
        assertEquals("Acme Global International LLC", response.legalName)
        assertEquals("Acme Global", response.tradeName)
        assertEquals("EUR", response.baseCurrency)
        assertEquals(newFiscalStart, response.fiscalYearStart)
        assertEquals("America/New_York", response.timezone)
        assertEquals("https://example.com/new-logo.svg", response.logoUrl)
        assertEquals(InventoryCostingMethod.FIFO, response.inventoryCostingMethod)
        assertTrue(response.inventoryGlPostingEnabled)
    }

    @Test
    fun `updateSettings partial update preserves unmentioned fields`() {
        val org = createOrg()
        `when`(organizationRepository.findById(orgId)).thenReturn(Optional.of(org))
        `when`(organizationRepository.save(any<Organizations>())).thenAnswer { it.arguments[0] }

        val request =
            UpdateOrganizationSettingsRequest(
                timezone = "Asia/Tokyo",
                logoUrl = "https://example.com/updated-logo.png",
            )

        val response = service.updateSettings(orgId, request)

        assertEquals("Acme Corp", response.name)
        assertEquals("USD", response.baseCurrency)
        assertEquals("Asia/Tokyo", response.timezone)
        assertEquals("https://example.com/updated-logo.png", response.logoUrl)
    }

    @Test
    fun `updateSettings allows keeping identical name without duplicate error`() {
        val org = createOrg()
        `when`(organizationRepository.findById(orgId)).thenReturn(Optional.of(org))
        `when`(organizationRepository.save(any<Organizations>())).thenAnswer { it.arguments[0] }

        val request = UpdateOrganizationSettingsRequest(name = "Acme Corp")
        val response = service.updateSettings(orgId, request)

        assertEquals("Acme Corp", response.name)
    }

    @Test
    fun `updateSettings throws BusinessRuleException when name is duplicate`() {
        val org = createOrg()
        `when`(organizationRepository.findById(orgId)).thenReturn(Optional.of(org))
        `when`(organizationRepository.existsByName("Existing Corp")).thenReturn(true)

        val request = UpdateOrganizationSettingsRequest(name = "Existing Corp")

        val exception =
            assertThrows<BusinessRuleException> {
                service.updateSettings(orgId, request)
            }
        assertTrue(exception.message!!.contains("already exists"))
    }

    @Test
    fun `updateSettings throws BusinessRuleException when name is blank`() {
        val org = createOrg()
        `when`(organizationRepository.findById(orgId)).thenReturn(Optional.of(org))

        val request = UpdateOrganizationSettingsRequest(name = "   ")

        val exception =
            assertThrows<BusinessRuleException> {
                service.updateSettings(orgId, request)
            }
        assertTrue(exception.message!!.contains("cannot be blank"))
    }

    @Test
    fun `updateSettings throws BusinessRuleException when currency is unsupported`() {
        val org = createOrg()
        `when`(organizationRepository.findById(orgId)).thenReturn(Optional.of(org))
        `when`(currencyRepository.existsById("XYZ")).thenReturn(false)

        val request = UpdateOrganizationSettingsRequest(baseCurrency = "XYZ")

        val exception =
            assertThrows<BusinessRuleException> {
                service.updateSettings(orgId, request)
            }
        assertTrue(exception.message!!.contains("Invalid or unsupported currency"))
    }

    @Test
    fun `updateSettings throws BusinessRuleException when timezone is invalid`() {
        val org = createOrg()
        `when`(organizationRepository.findById(orgId)).thenReturn(Optional.of(org))

        val request = UpdateOrganizationSettingsRequest(timezone = "Not/A_Real_Timezone")

        val exception =
            assertThrows<BusinessRuleException> {
                service.updateSettings(orgId, request)
            }
        assertTrue(exception.message!!.contains("Invalid timezone"))
    }
}
