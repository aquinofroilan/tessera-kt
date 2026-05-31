package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateVendorRequest
import com.aquinofroilan.tessera.dto.UpdateVendorRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.Vendor
import com.aquinofroilan.tessera.repository.VendorRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import java.util.Optional

class VendorServiceTest {
    private lateinit var vendorService: VendorService
    private lateinit var vendorRepository: VendorRepository

    private val orgId = "org-123"

    @BeforeEach
    fun setup() {
        vendorRepository = mock(VendorRepository::class.java)
        vendorService = VendorService(vendorRepository)
    }

    @Test
    fun `create should save vendor with correct fields`() {
        `when`(vendorRepository.save(any<Vendor>())).thenAnswer { it.arguments[0] }

        val request =
            CreateVendorRequest(
                name = "Acme Corp",
                contactName = "John Doe",
                contactEmail = "john@acme.com",
                paymentTermDays = 45,
            )

        val result = vendorService.createVendor(request, orgId)

        assertThat(result.name).isEqualTo("Acme Corp")
        assertThat(result.contactName).isEqualTo("John Doe")
        assertThat(result.contactEmail).isEqualTo("john@acme.com")
        assertThat(result.paymentTermDays).isEqualTo(45)
        assertThat(result.organizationId).isEqualTo(orgId)
        assertThat(result.isActive).isTrue()
    }

    @Test
    fun `get should return vendor for correct org`() {
        val vendor = createVendor()
        `when`(vendorRepository.findById("v-1")).thenReturn(Optional.of(vendor))

        val result = vendorService.getVendor("v-1", orgId)
        assertThat(result.id).isEqualTo("v-1")
    }

    @Test
    fun `get should throw when vendor belongs to different org`() {
        val vendor = createVendor(orgId = "other-org")
        `when`(vendorRepository.findById("v-1")).thenReturn(Optional.of(vendor))

        val exception =
            assertThrows<ResourceNotFoundException> {
                vendorService.getVendor("v-1", orgId)
            }
        assertThat(exception.message).contains("Vendor not found")
    }

    @Test
    fun `list should return active vendors`() {
        val vendors = listOf(createVendor(), createVendor(id = "v-2", name = "Beta Inc"))
        `when`(vendorRepository.findByOrganizationIdAndIsActive(orgId, true)).thenReturn(vendors)

        val result = vendorService.listVendors(orgId)
        assertThat(result).hasSize(2)
    }

    @Test
    fun `update should apply partial changes`() {
        val vendor = createVendor()
        `when`(vendorRepository.findById("v-1")).thenReturn(Optional.of(vendor))
        `when`(vendorRepository.save(any<Vendor>())).thenAnswer { it.arguments[0] }

        val request = UpdateVendorRequest(name = "Updated Corp")
        val result = vendorService.updateVendor("v-1", request, orgId)

        assertThat(result.name).isEqualTo("Updated Corp")
        assertThat(result.contactName).isEqualTo("John Doe")
    }

    @Test
    fun `update should reject inactive vendor`() {
        val vendor = createVendor(isActive = false)
        `when`(vendorRepository.findById("v-1")).thenReturn(Optional.of(vendor))

        val exception =
            assertThrows<BusinessRuleException> {
                vendorService.updateVendor("v-1", UpdateVendorRequest(name = "New"), orgId)
            }
        assertThat(exception.message).contains("inactive")
    }

    @Test
    fun `delete should soft delete vendor`() {
        val vendor = createVendor()
        `when`(vendorRepository.findById("v-1")).thenReturn(Optional.of(vendor))
        `when`(vendorRepository.save(any<Vendor>())).thenAnswer { it.arguments[0] }

        val result = vendorService.deleteVendor("v-1", orgId)

        assertThat(result.isActive).isFalse()
        val captor = argumentCaptor<Vendor>()
        verify(vendorRepository).save(captor.capture())
        assertThat(captor.firstValue.isActive).isFalse()
    }

    @Test
    fun `delete should reject already inactive vendor`() {
        val vendor = createVendor(isActive = false)
        `when`(vendorRepository.findById("v-1")).thenReturn(Optional.of(vendor))

        val exception =
            assertThrows<BusinessRuleException> {
                vendorService.deleteVendor("v-1", orgId)
            }
        assertThat(exception.message).contains("already inactive")
    }

    private fun createVendor(
        id: String = "v-1",
        name: String = "Acme Corp",
        orgId: String = this.orgId,
        isActive: Boolean = true,
    ) = Vendor(
        id = id,
        name = name,
        contactName = "John Doe",
        contactEmail = "john@acme.com",
        paymentTermDays = 30,
        organizationId = orgId,
        isActive = isActive,
    )
}
