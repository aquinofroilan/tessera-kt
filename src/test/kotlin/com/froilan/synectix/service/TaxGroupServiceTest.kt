package com.froilan.synectix.service

import com.froilan.synectix.dto.AccountBalanceResponse
import com.froilan.synectix.dto.CreateTaxGroupRequest
import com.froilan.synectix.dto.UpdateTaxGroupRequest
import com.froilan.synectix.exception.BusinessRuleException
import com.froilan.synectix.model.Account
import com.froilan.synectix.model.AccountType
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
import java.time.LocalDate
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
    fun `getTaxSummary should compute net liability from balance deltas`() {
        val payable = createAccount("acc-2300", "2300", AccountType.LIABILITY)
        val input = createAccount("acc-2310", "2310", AccountType.ASSET)
        val startDate = LocalDate.of(2026, 3, 1)
        val endDate = LocalDate.of(2026, 3, 31)

        `when`(accountRepository.findByOrganizationIdAndCode(orgId, "2300"))
            .thenReturn(Optional.of(payable))
        `when`(accountRepository.findByOrganizationIdAndCode(orgId, "2310"))
            .thenReturn(Optional.of(input))

        `when`(journalEntryService.getAccountBalance("acc-2300", orgId, endDate))
            .thenReturn(balanceOf(payable, BigDecimal("500.00")))
        `when`(journalEntryService.getAccountBalance("acc-2300", orgId, startDate.minusDays(1)))
            .thenReturn(balanceOf(payable, BigDecimal("100.00")))
        `when`(journalEntryService.getAccountBalance("acc-2310", orgId, endDate))
            .thenReturn(balanceOf(input, BigDecimal("150.00")))
        `when`(journalEntryService.getAccountBalance("acc-2310", orgId, startDate.minusDays(1)))
            .thenReturn(balanceOf(input, BigDecimal("50.00")))

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

    private fun balanceOf(
        account: Account,
        balance: BigDecimal,
    ) = AccountBalanceResponse(
        accountId = account.id,
        accountCode = account.code,
        accountName = account.name,
        accountType = account.type.name,
        totalDebits = BigDecimal.ZERO,
        totalCredits = BigDecimal.ZERO,
        balance = balance,
    )
}
