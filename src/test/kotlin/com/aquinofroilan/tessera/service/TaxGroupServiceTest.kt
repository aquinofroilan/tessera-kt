package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateTaxGroupRequest
import com.aquinofroilan.tessera.dto.UpdateTaxGroupRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.model.Account
import com.aquinofroilan.tessera.model.AccountType
import com.aquinofroilan.tessera.model.TaxGroup
import com.aquinofroilan.tessera.model.TaxRate
import com.aquinofroilan.tessera.repository.AccountRepository
import com.aquinofroilan.tessera.repository.AccountTotals
import com.aquinofroilan.tessera.repository.JournalEntryRepository
import com.aquinofroilan.tessera.repository.TaxGroupRepository
import com.aquinofroilan.tessera.repository.TaxRateRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

class TaxGroupServiceTest {
    private lateinit var taxGroupService: TaxGroupService
    private lateinit var taxGroupRepository: TaxGroupRepository
    private lateinit var taxRateRepository: TaxRateRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var journalEntryRepository: JournalEntryRepository

    private val orgId = java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d")

    @BeforeEach
    fun setup() {
        taxGroupRepository = mock(TaxGroupRepository::class.java)
        taxRateRepository = mock(TaxRateRepository::class.java)
        accountRepository = mock(AccountRepository::class.java)
        journalEntryRepository = mock(JournalEntryRepository::class.java)
        taxGroupService =
            TaxGroupService(taxGroupRepository, taxRateRepository, accountRepository, journalEntryRepository)
    }

    @Test
    fun `create should compute combinedRate from member rates`() {
        val rate1 = createTaxRate(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"), BigDecimal("4.00"))
        val rate2 = createTaxRate(java.util.UUID.fromString("00000000-0000-0000-0000-000000000002"), BigDecimal("4.50"))

        `when`(taxRateRepository.findAllById(listOf(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"), java.util.UUID.fromString("00000000-0000-0000-0000-000000000002"))))
            .thenReturn(listOf(rate1, rate2))
        `when`(taxGroupRepository.save(any<TaxGroup>())).thenAnswer { it.arguments[0] }

        val request =
            CreateTaxGroupRequest(
                name = "NY Combined",
                code = "NYC",
                taxRateIds = listOf(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"), java.util.UUID.fromString("00000000-0000-0000-0000-000000000002")),
            )

        val result = taxGroupService.createTaxGroup(request, orgId)

        assertThat(result.combinedRate).isEqualByComparingTo(BigDecimal("8.50"))
        assertThat(result.taxRateIds).hasSize(2)
    }

    @Test
    fun `create should throw when taxRateId not found`() {
        val missingId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000999")
        `when`(taxRateRepository.findAllById(listOf(missingId)))
            .thenReturn(emptyList())

        val request =
            CreateTaxGroupRequest(
                name = "Bad Group",
                code = "BAD",
                taxRateIds = listOf(missingId),
            )

        val exception =
            assertThrows<BusinessRuleException> {
                taxGroupService.createTaxGroup(request, orgId)
            }
        assertThat(exception.message).contains("not found")
    }

    @Test
    fun `create should throw when taxRate belongs to different org`() {
        val rate = createTaxRate(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"), BigDecimal("5.00"), orgId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000099"))
        `when`(taxRateRepository.findAllById(listOf(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001")))).thenReturn(listOf(rate))

        val request =
            CreateTaxGroupRequest(
                name = "Wrong Org",
                code = "WO",
                taxRateIds = listOf(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001")),
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
                id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000011"),
                name = "Old Group",
                code = "OLD",
                taxRateIds = listOf(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001")),
                combinedRate = BigDecimal("4.00"),
                organizationId = orgId,
            )
        val rate1 = createTaxRate(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"), BigDecimal("4.00"))
        val rate2 = createTaxRate(java.util.UUID.fromString("00000000-0000-0000-0000-000000000002"), BigDecimal("6.00"))

        `when`(taxGroupRepository.findById(java.util.UUID.fromString("00000000-0000-0000-0000-000000000011"))).thenReturn(Optional.of(group))
        `when`(taxRateRepository.findAllById(listOf(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"), java.util.UUID.fromString("00000000-0000-0000-0000-000000000002"))))
            .thenReturn(listOf(rate1, rate2))
        `when`(taxGroupRepository.save(any<TaxGroup>())).thenAnswer { it.arguments[0] }

        val result =
            taxGroupService.updateTaxGroup(
                java.util.UUID.fromString("00000000-0000-0000-0000-000000000011"),
                UpdateTaxGroupRequest(taxRateIds = listOf(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"), java.util.UUID.fromString("00000000-0000-0000-0000-000000000002"))),
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
                id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000011"),
                name = "Test",
                code = "TST",
                taxRateIds = listOf(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001")),
                combinedRate = BigDecimal("8.50"),
                organizationId = orgId,
            )
        `when`(taxGroupRepository.findById(java.util.UUID.fromString("00000000-0000-0000-0000-000000000011"))).thenReturn(Optional.of(group))

        val result = taxGroupService.calculateTaxAmount(java.util.UUID.fromString("00000000-0000-0000-0000-000000000011"), orgId, BigDecimal("1000.00"))

        assertThat(result).isEqualByComparingTo(BigDecimal("85.00"))
    }

    @Test
    fun `calculateTaxAmount should round to 2 decimal places`() {
        val group =
            TaxGroup(
                id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000011"),
                name = "Test",
                code = "TST",
                taxRateIds = listOf(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001")),
                combinedRate = BigDecimal("8.333"),
                organizationId = orgId,
            )
        `when`(taxGroupRepository.findById(java.util.UUID.fromString("00000000-0000-0000-0000-000000000011"))).thenReturn(Optional.of(group))

        val result = taxGroupService.calculateTaxAmount(java.util.UUID.fromString("00000000-0000-0000-0000-000000000011"), orgId, BigDecimal("100.00"))

        assertThat(result).isEqualByComparingTo(BigDecimal("8.33"))
    }

    @Test
    fun `getTaxGroupWithRates should preserve taxRateIds order`() {
        val idA = java.util.UUID.fromString("00000000-0000-0000-0000-00000000000A")
        val idB = java.util.UUID.fromString("00000000-0000-0000-0000-00000000000B")
        val idC = java.util.UUID.fromString("00000000-0000-0000-0000-00000000000C")
        val group =
            TaxGroup(
                id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000011"),
                name = "Ordered Group",
                code = "ORD",
                taxRateIds = listOf(idC, idA, idB),
                combinedRate = BigDecimal("10.00"),
                organizationId = orgId,
            )
        val rateA = createTaxRate(idA, BigDecimal("3.00"))
        val rateB = createTaxRate(idB, BigDecimal("3.00"))
        val rateC = createTaxRate(idC, BigDecimal("4.00"))

        `when`(taxGroupRepository.findById(java.util.UUID.fromString("00000000-0000-0000-0000-000000000011"))).thenReturn(Optional.of(group))
        `when`(taxRateRepository.findAllById(listOf(idC, idA, idB)))
            .thenReturn(listOf(rateA, rateB, rateC))

        val (_, rates) = taxGroupService.getTaxGroupWithRates(java.util.UUID.fromString("00000000-0000-0000-0000-000000000011"), orgId)

        assertThat(rates.map { it.id }).containsExactly(idC, idA, idB)
    }

    @Test
    fun `getTaxGroupWithRates should reject cross-org rate ids`() {
        val foreignId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000999")
        val group =
            TaxGroup(
                id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000011"),
                name = "Group",
                code = "GRP",
                taxRateIds = listOf(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"), foreignId),
                combinedRate = BigDecimal("10.00"),
                organizationId = orgId,
            )
        val rate1 = createTaxRate(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"), BigDecimal("5.00"))
        val foreignRate = createTaxRate(foreignId, BigDecimal("5.00"), orgId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000099"))

        `when`(taxGroupRepository.findById(java.util.UUID.fromString("00000000-0000-0000-0000-000000000011"))).thenReturn(Optional.of(group))
        `when`(taxRateRepository.findAllById(listOf(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"), foreignId)))
            .thenReturn(listOf(rate1, foreignRate))

        val exception =
            assertThrows<BusinessRuleException> {
                taxGroupService.getTaxGroupWithRates(java.util.UUID.fromString("00000000-0000-0000-0000-000000000011"), orgId)
            }
        assertThat(exception.message).contains(foreignId.toString())
    }

    @Test
    fun `update without taxRateIds should validate existing rates and reject cross-org`() {
        val foreignId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000999")
        val group =
            TaxGroup(
                id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000011"),
                name = "Existing",
                code = "EXG",
                taxRateIds = listOf(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"), foreignId),
                combinedRate = BigDecimal("10.00"),
                organizationId = orgId,
            )
        val rate1 = createTaxRate(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"), BigDecimal("5.00"))
        val foreignRate = createTaxRate(foreignId, BigDecimal("5.00"), orgId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000099"))

        `when`(taxGroupRepository.findById(java.util.UUID.fromString("00000000-0000-0000-0000-000000000011"))).thenReturn(Optional.of(group))
        `when`(taxRateRepository.findAllById(listOf(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"), foreignId)))
            .thenReturn(listOf(rate1, foreignRate))

        val exception =
            assertThrows<BusinessRuleException> {
                taxGroupService.updateTaxGroup(
                    java.util.UUID.fromString("00000000-0000-0000-0000-000000000011"),
                    UpdateTaxGroupRequest(name = "Renamed"),
                    orgId,
                )
            }
        assertThat(exception.message).contains("not found")
    }

    @Test
    fun `getTaxGroupWithRates should throw when referenced rate is missing`() {
        val missingId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000999")
        val group =
            TaxGroup(
                id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000011"),
                name = "Broken Group",
                code = "BRK",
                taxRateIds = listOf(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"), missingId),
                combinedRate = BigDecimal("5.00"),
                organizationId = orgId,
            )
        val rate1 = createTaxRate(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"), BigDecimal("5.00"))

        `when`(taxGroupRepository.findById(java.util.UUID.fromString("00000000-0000-0000-0000-000000000011"))).thenReturn(Optional.of(group))
        `when`(taxRateRepository.findAllById(listOf(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"), missingId)))
            .thenReturn(listOf(rate1))

        val exception =
            assertThrows<BusinessRuleException> {
                taxGroupService.getTaxGroupWithRates(java.util.UUID.fromString("00000000-0000-0000-0000-000000000011"), orgId)
            }
        assertThat(exception.message).contains(missingId.toString())
    }

    @Test
    fun `getTaxSummary should reject when start date is after end date`() {
        val exception =
            assertThrows<BusinessRuleException> {
                taxGroupService.getTaxSummary(
                    orgId,
                    startDate = LocalDate.of(2026, 4, 1),
                    endDate = LocalDate.of(2026, 3, 1),
                )
            }
        assertThat(exception.message).contains("on or before")
    }

    @Test
    fun `getTaxSummary should return zero when tax accounts not configured`() {
        `when`(accountRepository.findByOrganizationIdAndCode(orgId, "2300"))
            .thenReturn(Optional.empty())
        `when`(accountRepository.findByOrganizationIdAndCode(orgId, "2310"))
            .thenReturn(Optional.empty())

        val result =
            taxGroupService.getTaxSummary(
                orgId,
                startDate = LocalDate.of(2026, 3, 1),
                endDate = LocalDate.of(2026, 3, 31),
            )

        assertThat(result.taxCollected).isEqualByComparingTo(BigDecimal.ZERO)
        assertThat(result.taxPaid).isEqualByComparingTo(BigDecimal.ZERO)
        assertThat(result.netTaxLiability).isEqualByComparingTo(BigDecimal.ZERO)
    }

    @Test
    fun `getTaxSummary should compute net liability from period totals`() {
        val payable = createAccount(java.util.UUID.fromString("00000000-0000-0000-0000-000000000021"), "2300", AccountType.LIABILITY)
        val input = createAccount(java.util.UUID.fromString("00000000-0000-0000-0000-000000000022"), "2310", AccountType.ASSET)
        val startDate = LocalDate.of(2026, 3, 1)
        val endDate = LocalDate.of(2026, 3, 31)

        `when`(accountRepository.findByOrganizationIdAndCode(orgId, "2300"))
            .thenReturn(Optional.of(payable))
        `when`(accountRepository.findByOrganizationIdAndCode(orgId, "2310"))
            .thenReturn(Optional.of(input))

        `when`(
            journalEntryRepository.aggregateAccountTotals(
                orgId,
                listOf(java.util.UUID.fromString("00000000-0000-0000-0000-000000000021"), java.util.UUID.fromString("00000000-0000-0000-0000-000000000022")),
                startDate,
                endDate,
            ),
        ).thenReturn(
            mapOf(
                java.util.UUID.fromString("00000000-0000-0000-0000-000000000021") to AccountTotals(BigDecimal("0.00"), BigDecimal("400.00")),
                java.util.UUID.fromString("00000000-0000-0000-0000-000000000022") to AccountTotals(BigDecimal("100.00"), BigDecimal("0.00")),
            ),
        )

        val result = taxGroupService.getTaxSummary(orgId, startDate, endDate)

        assertThat(result.taxCollected).isEqualByComparingTo(BigDecimal("400.00"))
        assertThat(result.taxPaid).isEqualByComparingTo(BigDecimal("100.00"))
        assertThat(result.netTaxLiability).isEqualByComparingTo(BigDecimal("300.00"))
        assertThat(result.startDate).isEqualTo(startDate.toString())
        assertThat(result.endDate).isEqualTo(endDate.toString())
    }

    private fun createTaxRate(
        id: java.util.UUID = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"),
        percentage: BigDecimal = BigDecimal("8.00"),
        orgId: java.util.UUID = this.orgId,
    ) = TaxRate(
        id = id,
        name = "Tax Rate ${id}",
        code = "TR-${id}",
        percentage = percentage,
        authority = "State",
        organizationId = orgId,
    )

    private fun createAccount(
        id: java.util.UUID,
        code: String,
        type: AccountType,
    ) = Account(
        id = id,
        code = code,
        name = "Account ${code}",
        type = type,
        organizationId = orgId,
    )
}
