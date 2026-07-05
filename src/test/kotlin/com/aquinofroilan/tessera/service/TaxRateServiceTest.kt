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

    private val orgId = "org-123"

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
        val rate = createTaxRate(orgId = "other-org")
        `when`(taxRateRepository.findById("tr-1")).thenReturn(Optional.of(rate))

        assertThrows<ResourceNotFoundException> {
            taxRateService.getTaxRate("tr-1", orgId)
        }
    }

    @Test
    fun `update percentage should cascade to tax groups`() {
        val rate = createTaxRate()
        val group =
            TaxGroup(
                id = "tg-1",
                name = "Combined",
                code = "COMB",
                taxRateIds = listOf("tr-1"),
                combinedRate = BigDecimal("8.00"),
                organizationId = orgId,
            )
        val updatedRate = createTaxRate(percentage = BigDecimal("10.00"))

        `when`(taxRateRepository.findById("tr-1")).thenReturn(Optional.of(rate))
        `when`(taxRateRepository.save(any<TaxRate>())).thenReturn(updatedRate)
        `when`(taxGroupRepository.findByOrganizationIdAndTaxRateIdsContaining(orgId, "tr-1"))
            .thenReturn(listOf(group))
        `when`(taxRateRepository.findAllById(listOf("tr-1"))).thenReturn(listOf(updatedRate))
        `when`(taxGroupRepository.save(any<TaxGroup>())).thenAnswer { it.arguments[0] }

        taxRateService.updateTaxRate("tr-1", UpdateTaxRateRequest(percentage = BigDecimal("10.00")), orgId)

        verify(taxGroupRepository).save(any<TaxGroup>())
    }

    @Test
    fun `update percentage should fail-fast when cascade encounters missing rate`() {
        val rate = createTaxRate()
        val group =
            TaxGroup(
                id = "tg-1",
                name = "Combined",
                code = "COMB",
                taxRateIds = listOf("tr-1", "tr-orphan"),
                combinedRate = BigDecimal("12.00"),
                organizationId = orgId,
            )
        val updatedRate = createTaxRate(percentage = BigDecimal("10.00"))

        `when`(taxRateRepository.findById("tr-1")).thenReturn(Optional.of(rate))
        `when`(taxRateRepository.save(any<TaxRate>())).thenReturn(updatedRate)
        `when`(taxGroupRepository.findByOrganizationIdAndTaxRateIdsContaining(orgId, "tr-1"))
            .thenReturn(listOf(group))
        `when`(taxRateRepository.findAllById(listOf("tr-1", "tr-orphan")))
            .thenReturn(listOf(updatedRate))

        val exception =
            assertThrows<BusinessRuleException> {
                taxRateService.updateTaxRate(
                    "tr-1",
                    UpdateTaxRateRequest(percentage = BigDecimal("10.00")),
                    orgId,
                )
            }
        assertThat(exception.message).contains("tr-orphan")
    }

    @Test
    fun `delete should throw when rate is in active group`() {
        val rate = createTaxRate()
        val activeGroup =
            TaxGroup(
                id = "tg-1",
                name = "Combined",
                code = "COMB",
                taxRateIds = listOf("tr-1"),
                combinedRate = BigDecimal("8.00"),
                organizationId = orgId,
            )

        `when`(taxRateRepository.findById("tr-1")).thenReturn(Optional.of(rate))
        `when`(taxGroupRepository.findByOrganizationIdAndTaxRateIdsContaining(orgId, "tr-1"))
            .thenReturn(listOf(activeGroup))

        val exception =
            assertThrows<BusinessRuleException> {
                taxRateService.deleteTaxRate("tr-1", orgId)
            }
        assertThat(exception.message).contains("active tax groups")
    }

    @Test
    fun `delete should soft-delete when not in any group`() {
        val rate = createTaxRate()

        `when`(taxRateRepository.findById("tr-1")).thenReturn(Optional.of(rate))
        `when`(taxGroupRepository.findByOrganizationIdAndTaxRateIdsContaining(orgId, "tr-1"))
            .thenReturn(emptyList())
        `when`(taxRateRepository.save(any<TaxRate>())).thenAnswer { it.arguments[0] }

        val result = taxRateService.deleteTaxRate("tr-1", orgId)
        assertThat(result.isActive).isFalse()
    }

    private fun createTaxRate(
        id: String = "tr-1",
        percentage: BigDecimal = BigDecimal("8.00"),
        orgId: String = this.orgId,
    ) = TaxRate(
        id = id,
        name = "State Sales Tax",
        code = "SST",
        percentage = percentage,
        authority = "State",
        organizationId = orgId,
    )
}
