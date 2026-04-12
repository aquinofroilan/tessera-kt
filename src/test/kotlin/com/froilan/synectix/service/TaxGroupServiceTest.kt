package com.froilan.synectix.service

import com.froilan.synectix.dto.CreateTaxGroupRequest
import com.froilan.synectix.dto.UpdateTaxGroupRequest
import com.froilan.synectix.exception.BusinessRuleException
import com.froilan.synectix.model.TaxGroup
import com.froilan.synectix.model.TaxRate
import com.froilan.synectix.repository.AccountRepository
import com.froilan.synectix.repository.TaxGroupRepository
import com.froilan.synectix.repository.TaxRateRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import java.math.BigDecimal
import java.util.Optional

class TaxGroupServiceTest {
    private lateinit var taxGroupService: TaxGroupService
    private lateinit var taxGroupRepository: TaxGroupRepository
    private lateinit var taxRateRepository: TaxRateRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var journalEntryService: JournalEntryService

    private val orgId = "org-123"

    @BeforeEach
    fun setup() {
        taxGroupRepository = mock(TaxGroupRepository::class.java)
        taxRateRepository = mock(TaxRateRepository::class.java)
        accountRepository = mock(AccountRepository::class.java)
        journalEntryService = mock(JournalEntryService::class.java)
        taxGroupService = TaxGroupService(taxGroupRepository, taxRateRepository, accountRepository, journalEntryService)
    }

    @Test
    fun `create should compute combinedRate from member rates`() {
        val rate1 = createTaxRate("tr-1", BigDecimal("4.00"))
        val rate2 = createTaxRate("tr-2", BigDecimal("4.50"))

        `when`(taxRateRepository.findAllById(listOf("tr-1", "tr-2")))
            .thenReturn(listOf(rate1, rate2))
        `when`(taxGroupRepository.save(any<TaxGroup>())).thenAnswer { it.arguments[0] }

        val request =
            CreateTaxGroupRequest(
                name = "NY Combined",
                code = "NYC",
                taxRateIds = listOf("tr-1", "tr-2"),
            )

        val result = taxGroupService.createTaxGroup(request, orgId)

        assertThat(result.combinedRate).isEqualByComparingTo(BigDecimal("8.50"))
        assertThat(result.taxRateIds).hasSize(2)
    }

    @Test
    fun `create should throw when taxRateId not found`() {
        `when`(taxRateRepository.findAllById(listOf("tr-missing")))
            .thenReturn(emptyList())

        val request =
            CreateTaxGroupRequest(
                name = "Bad Group",
                code = "BAD",
                taxRateIds = listOf("tr-missing"),
            )

        val exception =
            assertThrows<BusinessRuleException> {
                taxGroupService.createTaxGroup(request, orgId)
            }
        assertThat(exception.message).contains("not found")
    }

    @Test
    fun `create should throw when taxRate belongs to different org`() {
        val rate = createTaxRate("tr-1", BigDecimal("5.00"), orgId = "other-org")
        `when`(taxRateRepository.findAllById(listOf("tr-1"))).thenReturn(listOf(rate))

        val request =
            CreateTaxGroupRequest(
                name = "Wrong Org",
                code = "WO",
                taxRateIds = listOf("tr-1"),
            )

        val exception =
            assertThrows<BusinessRuleException> {
                taxGroupService.createTaxGroup(request, orgId)
            }
        assertThat(exception.message).contains("not found")
    }

    @Test
    fun `update taxRateIds should recompute combinedRate`() {
        val group =
            TaxGroup(
                id = "tg-1",
                name = "Old Group",
                code = "OLD",
                taxRateIds = listOf("tr-1"),
                combinedRate = BigDecimal("4.00"),
                organizationId = orgId,
            )
        val rate1 = createTaxRate("tr-1", BigDecimal("4.00"))
        val rate2 = createTaxRate("tr-2", BigDecimal("6.00"))

        `when`(taxGroupRepository.findById("tg-1")).thenReturn(Optional.of(group))
        `when`(taxRateRepository.findAllById(listOf("tr-1", "tr-2")))
            .thenReturn(listOf(rate1, rate2))
        `when`(taxGroupRepository.save(any<TaxGroup>())).thenAnswer { it.arguments[0] }

        val result =
            taxGroupService.updateTaxGroup(
                "tg-1",
                UpdateTaxGroupRequest(taxRateIds = listOf("tr-1", "tr-2")),
                orgId,
            )

        assertThat(result.combinedRate).isEqualByComparingTo(BigDecimal("10.00"))
    }

    @Test
    fun `calculateTaxAmount should return zero for null taxGroupId`() {
        val result = taxGroupService.calculateTaxAmount(null, orgId, BigDecimal("1000.00"))
        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO)
    }

    @Test
    fun `calculateTaxAmount should compute correctly with rounding`() {
        val group =
            TaxGroup(
                id = "tg-1",
                name = "Test",
                code = "TST",
                taxRateIds = listOf("tr-1"),
                combinedRate = BigDecimal("8.50"),
                organizationId = orgId,
            )
        `when`(taxGroupRepository.findById("tg-1")).thenReturn(Optional.of(group))

        val result = taxGroupService.calculateTaxAmount("tg-1", orgId, BigDecimal("1000.00"))

        assertThat(result).isEqualByComparingTo(BigDecimal("85.00"))
    }

    @Test
    fun `calculateTaxAmount should round to 2 decimal places`() {
        val group =
            TaxGroup(
                id = "tg-1",
                name = "Test",
                code = "TST",
                taxRateIds = listOf("tr-1"),
                combinedRate = BigDecimal("8.333"),
                organizationId = orgId,
            )
        `when`(taxGroupRepository.findById("tg-1")).thenReturn(Optional.of(group))

        val result = taxGroupService.calculateTaxAmount("tg-1", orgId, BigDecimal("100.00"))

        assertThat(result).isEqualByComparingTo(BigDecimal("8.33"))
    }

    private fun createTaxRate(
        id: String = "tr-1",
        percentage: BigDecimal = BigDecimal("8.00"),
        orgId: String = this.orgId,
    ) = TaxRate(
        id = id,
        name = "Tax Rate $id",
        code = "TR-$id",
        percentage = percentage,
        authority = "State",
        organizationId = orgId,
    )
}
