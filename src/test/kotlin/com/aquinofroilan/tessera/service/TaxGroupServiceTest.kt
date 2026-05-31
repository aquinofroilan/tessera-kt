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

    private val orgId = "org-123"

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

    @Test
    fun `getTaxGroupWithRates should preserve taxRateIds order`() {
        val group =
            TaxGroup(
                id = "tg-1",
                name = "Ordered Group",
                code = "ORD",
                taxRateIds = listOf("tr-c", "tr-a", "tr-b"),
                combinedRate = BigDecimal("10.00"),
                organizationId = orgId,
            )
        val rateA = createTaxRate("tr-a", BigDecimal("3.00"))
        val rateB = createTaxRate("tr-b", BigDecimal("3.00"))
        val rateC = createTaxRate("tr-c", BigDecimal("4.00"))

        `when`(taxGroupRepository.findById("tg-1")).thenReturn(Optional.of(group))
        `when`(taxRateRepository.findAllById(listOf("tr-c", "tr-a", "tr-b")))
            .thenReturn(listOf(rateA, rateB, rateC))

        val (_, rates) = taxGroupService.getTaxGroupWithRates("tg-1", orgId)

        assertThat(rates.map { it.id }).containsExactly("tr-c", "tr-a", "tr-b")
    }

    @Test
    fun `getTaxGroupWithRates should reject cross-org rate ids`() {
        val group =
            TaxGroup(
                id = "tg-1",
                name = "Group",
                code = "GRP",
                taxRateIds = listOf("tr-1", "tr-foreign"),
                combinedRate = BigDecimal("10.00"),
                organizationId = orgId,
            )
        val rate1 = createTaxRate("tr-1", BigDecimal("5.00"))
        val foreignRate = createTaxRate("tr-foreign", BigDecimal("5.00"), orgId = "other-org")

        `when`(taxGroupRepository.findById("tg-1")).thenReturn(Optional.of(group))
        `when`(taxRateRepository.findAllById(listOf("tr-1", "tr-foreign")))
            .thenReturn(listOf(rate1, foreignRate))

        val exception =
            assertThrows<BusinessRuleException> {
                taxGroupService.getTaxGroupWithRates("tg-1", orgId)
            }
        assertThat(exception.message).contains("tr-foreign")
    }

    @Test
    fun `update without taxRateIds should validate existing rates and reject cross-org`() {
        val group =
            TaxGroup(
                id = "tg-1",
                name = "Existing",
                code = "EXG",
                taxRateIds = listOf("tr-1", "tr-foreign"),
                combinedRate = BigDecimal("10.00"),
                organizationId = orgId,
            )
        val rate1 = createTaxRate("tr-1", BigDecimal("5.00"))
        val foreignRate = createTaxRate("tr-foreign", BigDecimal("5.00"), orgId = "other-org")

        `when`(taxGroupRepository.findById("tg-1")).thenReturn(Optional.of(group))
        `when`(taxRateRepository.findAllById(listOf("tr-1", "tr-foreign")))
            .thenReturn(listOf(rate1, foreignRate))

        val exception =
            assertThrows<BusinessRuleException> {
                taxGroupService.updateTaxGroup(
                    "tg-1",
                    UpdateTaxGroupRequest(name = "Renamed"),
                    orgId,
                )
            }
        assertThat(exception.message).contains("not found")
    }

    @Test
    fun `getTaxGroupWithRates should throw when referenced rate is missing`() {
        val group =
            TaxGroup(
                id = "tg-1",
                name = "Broken Group",
                code = "BRK",
                taxRateIds = listOf("tr-1", "tr-missing"),
                combinedRate = BigDecimal("5.00"),
                organizationId = orgId,
            )
        val rate1 = createTaxRate("tr-1", BigDecimal("5.00"))

        `when`(taxGroupRepository.findById("tg-1")).thenReturn(Optional.of(group))
        `when`(taxRateRepository.findAllById(listOf("tr-1", "tr-missing")))
            .thenReturn(listOf(rate1))

        val exception =
            assertThrows<BusinessRuleException> {
                taxGroupService.getTaxGroupWithRates("tg-1", orgId)
            }
        assertThat(exception.message).contains("tr-missing")
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
        val payable = createAccount("acc-2300", "2300", AccountType.LIABILITY)
        val input = createAccount("acc-2310", "2310", AccountType.ASSET)
        val startDate = LocalDate.of(2026, 3, 1)
        val endDate = LocalDate.of(2026, 3, 31)

        `when`(accountRepository.findByOrganizationIdAndCode(orgId, "2300"))
            .thenReturn(Optional.of(payable))
        `when`(accountRepository.findByOrganizationIdAndCode(orgId, "2310"))
            .thenReturn(Optional.of(input))

        `when`(
            journalEntryRepository.aggregateAccountTotals(
                orgId,
                listOf("acc-2300", "acc-2310"),
                startDate,
                endDate,
            ),
        ).thenReturn(
            mapOf(
                "acc-2300" to AccountTotals(BigDecimal("0.00"), BigDecimal("400.00")),
                "acc-2310" to AccountTotals(BigDecimal("100.00"), BigDecimal("0.00")),
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

    private fun createAccount(
        id: String,
        code: String,
        type: AccountType,
    ) = Account(
        id = id,
        code = code,
        name = "Account $code",
        type = type,
        organizationId = orgId,
    )
}
