package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateTaxRateRequest
import com.aquinofroilan.tessera.dto.UpdateTaxRateRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.TaxGroup
import com.aquinofroilan.tessera.model.TaxRate
import com.aquinofroilan.tessera.repository.TaxGroupRepository
import com.aquinofroilan.tessera.repository.TaxRateRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import java.math.BigDecimal
import java.util.Optional

class TaxRateServiceTest {
    private lateinit var taxRateService: TaxRateService
    private lateinit var taxRateRepository: TaxRateRepository
    private lateinit var taxGroupRepository: TaxGroupRepository

    private val orgId = java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d")

    @BeforeEach
    fun setup() {
        taxRateRepository = mock(TaxRateRepository::class.java)
        taxGroupRepository = mock(TaxGroupRepository::class.java)
        taxRateService = TaxRateService(taxRateRepository, taxGroupRepository)
    }

    @Test
    fun `create should save tax rate with correct fields`() {
        `when`(taxRateRepository.save(any<TaxRate>())).thenAnswer { it.arguments[0] }

        val request =
            CreateTaxRateRequest(
                name = "State Sales Tax",
                code = "SST",
                percentage = BigDecimal("8.00"),
                authority = "State",
            )

        val result = taxRateService.createTaxRate(request, orgId)

        assertThat(result.name).isEqualTo("State Sales Tax")
        assertThat(result.code).isEqualTo("SST")
        assertThat(result.percentage).isEqualByComparingTo(BigDecimal("8.00"))
        assertThat(result.authority).isEqualTo("State")
        assertThat(result.isActive).isTrue()
    }

    @Test
    fun `get should throw ResourceNotFoundException for wrong org`() {
        val rate = createTaxRate(orgId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000099"))
        `when`(taxRateRepository.findById(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"))).thenReturn(Optional.of(rate))

        assertThrows<ResourceNotFoundException> {
            taxRateService.getTaxRate(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"), orgId)
        }
    }

    @Test
    fun `update percentage should cascade to tax groups`() {
        val rate = createTaxRate()
        val group =
            TaxGroup(
                id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000011"),
                name = "Combined",
                code = "COMB",
                taxRateIds = listOf(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001")),
                combinedRate = BigDecimal("8.00"),
                organizationId = orgId,
            )
        val updatedRate = createTaxRate(percentage = BigDecimal("10.00"))

        `when`(taxRateRepository.findById(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"))).thenReturn(Optional.of(rate))
        `when`(taxRateRepository.save(any<TaxRate>())).thenReturn(updatedRate)
        `when`(taxGroupRepository.findByOrganizationIdAndTaxRateIdsContaining(orgId, java.util.UUID.fromString("00000000-0000-0000-0000-000000000001")))
            .thenReturn(listOf(group))
        `when`(taxRateRepository.findAllById(listOf(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001")))).thenReturn(listOf(updatedRate))
        `when`(taxGroupRepository.save(any<TaxGroup>())).thenAnswer { it.arguments[0] }

        taxRateService.updateTaxRate(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"), UpdateTaxRateRequest(percentage = BigDecimal("10.00")), orgId)

        verify(taxGroupRepository).save(any<TaxGroup>())
    }

    @Test
    fun `update percentage should fail-fast when cascade encounters missing rate`() {
        val orphanId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000999")
        val rate = createTaxRate()
        val group =
            TaxGroup(
                id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000011"),
                name = "Combined",
                code = "COMB",
                taxRateIds = listOf(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"), orphanId),
                combinedRate = BigDecimal("12.00"),
                organizationId = orgId,
            )
        val updatedRate = createTaxRate(percentage = BigDecimal("10.00"))

        `when`(taxRateRepository.findById(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"))).thenReturn(Optional.of(rate))
        `when`(taxRateRepository.save(any<TaxRate>())).thenReturn(updatedRate)
        `when`(taxGroupRepository.findByOrganizationIdAndTaxRateIdsContaining(orgId, java.util.UUID.fromString("00000000-0000-0000-0000-000000000001")))
            .thenReturn(listOf(group))
        `when`(taxRateRepository.findAllById(listOf(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"), orphanId)))
            .thenReturn(listOf(updatedRate))

        val exception =
            assertThrows<BusinessRuleException> {
                taxRateService.updateTaxRate(
                    java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"),
                    UpdateTaxRateRequest(percentage = BigDecimal("10.00")),
                    orgId,
                )
            }
        assertThat(exception.message).contains(orphanId.toString())
    }

    @Test
    fun `delete should throw when rate is in active group`() {
        val rate = createTaxRate()
        val activeGroup =
            TaxGroup(
                id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000011"),
                name = "Combined",
                code = "COMB",
                taxRateIds = listOf(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001")),
                combinedRate = BigDecimal("8.00"),
                organizationId = orgId,
            )

        `when`(taxRateRepository.findById(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"))).thenReturn(Optional.of(rate))
        `when`(taxGroupRepository.findByOrganizationIdAndTaxRateIdsContaining(orgId, java.util.UUID.fromString("00000000-0000-0000-0000-000000000001")))
            .thenReturn(listOf(activeGroup))

        val exception =
            assertThrows<BusinessRuleException> {
                taxRateService.deleteTaxRate(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"), orgId)
            }
        assertThat(exception.message).contains("active tax groups")
    }

    @Test
    fun `delete should soft-delete when not in any group`() {
        val rate = createTaxRate()

        `when`(taxRateRepository.findById(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"))).thenReturn(Optional.of(rate))
        `when`(taxGroupRepository.findByOrganizationIdAndTaxRateIdsContaining(orgId, java.util.UUID.fromString("00000000-0000-0000-0000-000000000001")))
            .thenReturn(emptyList())
        `when`(taxRateRepository.save(any<TaxRate>())).thenAnswer { it.arguments[0] }

        val result = taxRateService.deleteTaxRate(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"), orgId)
        assertThat(result.isActive).isFalse()
    }

    private fun createTaxRate(
        id: java.util.UUID = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"),
        percentage: BigDecimal = BigDecimal("8.00"),
        orgId: java.util.UUID = this.orgId,
    ) = TaxRate(
        id = id,
        name = "State Sales Tax",
        code = "SST",
        percentage = percentage,
        authority = "State",
        organizationId = orgId,
    )
}
