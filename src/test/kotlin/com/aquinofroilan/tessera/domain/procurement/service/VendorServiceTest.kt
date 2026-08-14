package com.aquinofroilan.tessera.domain.procurement.service

import com.aquinofroilan.tessera.domain.procurement.dto.CreateVendorRequest
import com.aquinofroilan.tessera.domain.procurement.dto.UpdateVendorRequest
import com.aquinofroilan.tessera.domain.procurement.model.Vendor
import com.aquinofroilan.tessera.domain.procurement.repository.VendorRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
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

    private val orgId = java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d")

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
        val vendor = createVendor(id = java.util.UUID.fromString("718fa2b3-0eb7-3a9c-987f-a0cbe216ac6a"))
        `when`(vendorRepository.findById(java.util.UUID.fromString("718fa2b3-0eb7-3a9c-987f-a0cbe216ac6a"))).thenReturn(Optional.of(vendor))

        val result = vendorService.getVendor(java.util.UUID.fromString("718fa2b3-0eb7-3a9c-987f-a0cbe216ac6a"), orgId)
        assertThat(result.id).isEqualTo(java.util.UUID.fromString("718fa2b3-0eb7-3a9c-987f-a0cbe216ac6a"))
    }

    @Test
    fun `get should throw when vendor belongs to different org`() {
        val vendor = createVendor(orgId = java.util.UUID.fromString("fbede99a-0bef-3bf9-ba0b-8d28f050479d"))
        `when`(vendorRepository.findById(java.util.UUID.fromString("718fa2b3-0eb7-3a9c-987f-a0cbe216ac6a"))).thenReturn(Optional.of(vendor))

        val exception =
            assertThrows<ResourceNotFoundException> {
                vendorService.getVendor(java.util.UUID.fromString("718fa2b3-0eb7-3a9c-987f-a0cbe216ac6a"), orgId)
            }
        assertThat(exception.message).contains("Vendor not found")
    }

    @Test
    fun `list should return active vendors`() {
        val vendors = listOf(createVendor(), createVendor(id = java.util.UUID.ofEpochMillis(System.currentTimeMillis())))
        `when`(vendorRepository.findByOrganizationIdAndIsActive(orgId, true)).thenReturn(vendors)

        val result = vendorService.listVendors(orgId)
        assertThat(result).hasSize(2)
    }

    @Test
    fun `update should apply partial changes`() {
        val vendor = createVendor()
        `when`(vendorRepository.findById(java.util.UUID.fromString("718fa2b3-0eb7-3a9c-987f-a0cbe216ac6a"))).thenReturn(Optional.of(vendor))
        `when`(vendorRepository.save(any<Vendor>())).thenAnswer { it.arguments[0] }

        val request = UpdateVendorRequest(name = "Updated Corp")
        val result = vendorService.updateVendor(java.util.UUID.fromString("718fa2b3-0eb7-3a9c-987f-a0cbe216ac6a"), request, orgId)

        assertThat(result.name).isEqualTo("Updated Corp")
        assertThat(result.contactName).isEqualTo("John Doe")
    }

    @Test
    fun `update should reject inactive vendor`() {
        val vendor = createVendor(isActive = false)
        `when`(vendorRepository.findById(java.util.UUID.fromString("718fa2b3-0eb7-3a9c-987f-a0cbe216ac6a"))).thenReturn(Optional.of(vendor))

        val exception =
            assertThrows<BusinessRuleException> {
                vendorService.updateVendor(
                    java.util.UUID.fromString("718fa2b3-0eb7-3a9c-987f-a0cbe216ac6a"),
                    UpdateVendorRequest(name = "New"),
                    orgId,
                )
            }
        assertThat(exception.message).contains("inactive")
    }

    @Test
    fun `delete should soft delete vendor`() {
        val vendor = createVendor()
        `when`(vendorRepository.findById(java.util.UUID.fromString("718fa2b3-0eb7-3a9c-987f-a0cbe216ac6a"))).thenReturn(Optional.of(vendor))
        `when`(vendorRepository.save(any<Vendor>())).thenAnswer { it.arguments[0] }

        val result = vendorService.deleteVendor(java.util.UUID.fromString("718fa2b3-0eb7-3a9c-987f-a0cbe216ac6a"), orgId)

        assertThat(result.isActive).isFalse()
        val captor = argumentCaptor<Vendor>()
        verify(vendorRepository).save(captor.capture())
        assertThat(captor.firstValue.isActive).isFalse()
    }

    @Test
    fun `delete should reject already inactive vendor`() {
        val vendor = createVendor(isActive = false)
        `when`(vendorRepository.findById(java.util.UUID.fromString("718fa2b3-0eb7-3a9c-987f-a0cbe216ac6a"))).thenReturn(Optional.of(vendor))

        val exception =
            assertThrows<BusinessRuleException> {
                vendorService.deleteVendor(java.util.UUID.fromString("718fa2b3-0eb7-3a9c-987f-a0cbe216ac6a"), orgId)
            }
        assertThat(exception.message).contains("already inactive")
    }

    private fun createVendor(
        id: java.util.UUID = java.util.UUID.ofEpochMillis(System.currentTimeMillis()),
        name: String = "Acme Corp",
        orgId: java.util.UUID = this.orgId,
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
