package com.aquinofroilan.tessera.service

import java.util.UUID

import com.aquinofroilan.tessera.dto.SyntheticAccountIds
import com.aquinofroilan.tessera.dto.TrialBalanceResponse
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.model.Account
import com.aquinofroilan.tessera.model.AccountType
import com.aquinofroilan.tessera.model.JournalEntry
import com.aquinofroilan.tessera.model.JournalEntryLine
import com.aquinofroilan.tessera.model.JournalEntryStatus
import com.aquinofroilan.tessera.repository.AccountRepository
import com.aquinofroilan.tessera.repository.AccountTotals
import com.aquinofroilan.tessera.repository.JournalEntryRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.LocalDate

class FinancialReportServiceTest {
    private lateinit var service: FinancialReportService
    private lateinit var journalEntryRepository: JournalEntryRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var journalEntryService: JournalEntryService

    private val orgId = java.util.UUID.fromString("e5628ca4-87a8-3e6f-8ae2-20213cc7ef92")
    private val userId = java.util.UUID.fromString("1db2395f-13ba-3d37-9d2b-f77d3eb3aa2e")

    @BeforeEach
    fun setup() {
        journalEntryRepository = mock(JournalEntryRepository::class.java)
        accountRepository = mock(AccountRepository::class.java)
        journalEntryService = mock(JournalEntryService::class.java)
        service = FinancialReportService(journalEntryRepository, accountRepository, journalEntryService)
    }

    @Test
    fun `getIncomeStatement computes net income and sorts by code`() {
        val start = LocalDate.of(2026, 3, 1)
        val end = LocalDate.of(2026, 3, 31)
        val revId = java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a")
        val expId = java.util.UUID.fromString("19770c7a-8b4f-3b6e-adb1-3631faff91ec")
        val revenue = account(revId, "4100", AccountType.REVENUE)
        val expense = account(expId, "5000", AccountType.EXPENSE)

        `when`(accountRepository.findByOrganizationIdAndIsActive(orgId, true))
            .thenReturn(listOf(expense, revenue))
        `when`(journalEntryRepository.aggregateAccountTotals(orgId, null, start, end))
            .thenReturn(
                mapOf(
                    revId to AccountTotals(BigDecimal.ZERO, BigDecimal("1000.00")),
                    expId to AccountTotals(BigDecimal("300.00"), BigDecimal.ZERO),
                ),
            )

        val result = service.getIncomeStatement(orgId, start, end)

        assertThat(result.revenue).hasSize(1)
        assertThat(result.revenue[0].accountCode).isEqualTo("4100")
        assertThat(result.revenue[0].amount).isEqualByComparingTo(BigDecimal("1000.00"))
        assertThat(result.expenses[0].amount).isEqualByComparingTo(BigDecimal("300.00"))
        assertThat(result.totalRevenue).isEqualByComparingTo(BigDecimal("1000.00"))
        assertThat(result.totalExpenses).isEqualByComparingTo(BigDecimal("300.00"))
        assertThat(result.netIncome).isEqualByComparingTo(BigDecimal("700.00"))
        assertThat(result.comparativeNetIncome).isNull()
    }

    @Test
    fun `getIncomeStatement with comparative period returns both totals`() {
        val start = LocalDate.of(2026, 3, 1)
        val end = LocalDate.of(2026, 3, 31)
        val compareStart = LocalDate.of(2026, 2, 1)
        val compareEnd = LocalDate.of(2026, 2, 28)
        val revId = java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a")
        val revenue = account(revId, "4100", AccountType.REVENUE)

        `when`(accountRepository.findByOrganizationIdAndIsActive(orgId, true))
            .thenReturn(listOf(revenue))
        `when`(journalEntryRepository.aggregateAccountTotals(orgId, null, start, end))
            .thenReturn(mapOf(java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a") to AccountTotals(BigDecimal.ZERO, BigDecimal("1000.00"))))
        `when`(journalEntryRepository.aggregateAccountTotals(orgId, null, compareStart, compareEnd))
            .thenReturn(mapOf(java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a") to AccountTotals(BigDecimal.ZERO, BigDecimal("800.00"))))

        val result = service.getIncomeStatement(orgId, start, end, compareStart, compareEnd)

        assertThat(result.totalRevenue).isEqualByComparingTo(BigDecimal("1000.00"))
        assertThat(result.comparativeTotalRevenue).isEqualByComparingTo(BigDecimal("800.00"))
        assertThat(result.netIncome).isEqualByComparingTo(BigDecimal("1000.00"))
        assertThat(result.comparativeNetIncome).isEqualByComparingTo(BigDecimal("800.00"))
        assertThat(result.comparativePeriod?.startDate).isEqualTo("2026-02-01")
    }

    @Test
    fun `getIncomeStatement rejects end before start`() {
        val exception =
            assertThrows<BusinessRuleException> {
                service.getIncomeStatement(orgId, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 3, 1))
            }
        assertThat(exception.message).contains("on or after")
    }

    @Test
    fun `getIncomeStatement rejects partial comparative dates`() {
        val exception =
            assertThrows<BusinessRuleException> {
                service.getIncomeStatement(
                    orgId,
                    LocalDate.of(2026, 3, 1),
                    LocalDate.of(2026, 3, 31),
                    compareStartDate = LocalDate.of(2026, 2, 1),
                    compareEndDate = null,
                )
            }
        assertThat(exception.message).contains("together")
    }

    @Test
    fun `getIncomeStatement returns empty lists when no accounts match`() {
        val start = LocalDate.of(2026, 3, 1)
        val end = LocalDate.of(2026, 3, 31)
        val cashId = java.util.UUID.fromString("f411ffb7-66f2-3534-92de-46af447dbec3")
        `when`(accountRepository.findByOrganizationIdAndIsActive(orgId, true))
            .thenReturn(listOf(account(cashId, "1000", AccountType.ASSET)))
        `when`(journalEntryRepository.aggregateAccountTotals(orgId, null, start, end))
            .thenReturn(emptyMap())

        val result = service.getIncomeStatement(orgId, start, end)

        assertThat(result.revenue).isEmpty()
        assertThat(result.expenses).isEmpty()
        assertThat(result.netIncome).isEqualByComparingTo(BigDecimal.ZERO)
    }

    @Test
    fun `getBalanceSheet computes totals and current earnings`() {
        val asOf = LocalDate.of(2026, 3, 31)
        val cashId = java.util.UUID.fromString("f411ffb7-66f2-3534-92de-46af447dbec3")
        val apId = java.util.UUID.fromString("97c02c2b-db1d-3201-b200-5645c65c4ecc")
        val eqId = java.util.UUID.fromString("8460eb67-cc2b-3de5-bdba-6c4320d2c3de")
        val revId = java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a")
        val expId = java.util.UUID.fromString("19770c7a-8b4f-3b6e-adb1-3631faff91ec")
        val cash = account(cashId, "1000", AccountType.ASSET)
        val ap = account(apId, "2000", AccountType.LIABILITY)
        val equity = account(eqId, "3000", AccountType.EQUITY)
        val rev = account(revId, "4000", AccountType.REVENUE)
        val exp = account(expId, "5000", AccountType.EXPENSE)

        `when`(accountRepository.findByOrganizationIdAndIsActive(orgId, true))
            .thenReturn(listOf(cash, ap, equity, rev, exp))
        `when`(journalEntryRepository.aggregateAccountTotals(orgId, null, null, asOf))
            .thenReturn(
                mapOf(
                    cashId to AccountTotals(BigDecimal("13000.00"), BigDecimal("500.00")),
                    eqId to AccountTotals(BigDecimal.ZERO, BigDecimal("10000.00")),
                    revId to AccountTotals(BigDecimal.ZERO, BigDecimal("3000.00")),
                    expId to AccountTotals(BigDecimal("500.00"), BigDecimal.ZERO),
                ),
            )

        val result = service.getBalanceSheet(orgId, asOf)

        assertThat(result.totalAssets).isEqualByComparingTo(BigDecimal("12500.00"))
        assertThat(result.totalLiabilities).isEqualByComparingTo(BigDecimal.ZERO)
        assertThat(result.currentEarnings).isEqualByComparingTo(BigDecimal("2500.00"))
        assertThat(result.totalEquity).isEqualByComparingTo(BigDecimal("12500.00"))
        assertThat(result.totalLiabilitiesAndEquity).isEqualByComparingTo(BigDecimal("12500.00"))
        assertThat(result.isBalanced).isTrue()
        assertThat(result.outOfBalanceAmount).isEqualByComparingTo(BigDecimal.ZERO)
        val earningsRow = result.equity.last()
        assertThat(earningsRow.accountName).isEqualTo("Current Period Earnings")
        assertThat(earningsRow.accountId).isEqualTo(SyntheticAccountIds.CURRENT_PERIOD_EARNINGS_ID)
        assertThat(earningsRow.accountCode).isEqualTo(SyntheticAccountIds.CURRENT_PERIOD_EARNINGS)
        assertThat(earningsRow.isSynthetic).isTrue()
    }

    @Test
    fun `getBalanceSheet flags imbalance without throwing`() {
        val asOf = LocalDate.of(2026, 3, 31)
        val cashId = java.util.UUID.fromString("f411ffb7-66f2-3534-92de-46af447dbec3")
        val eqId = java.util.UUID.fromString("8460eb67-cc2b-3de5-bdba-6c4320d2c3de")
        val cash = account(cashId, "1000", AccountType.ASSET)
        val equity = account(eqId, "3000", AccountType.EQUITY)

        `when`(accountRepository.findByOrganizationIdAndIsActive(orgId, true))
            .thenReturn(listOf(cash, equity))
        `when`(journalEntryRepository.aggregateAccountTotals(orgId, null, null, asOf))
            .thenReturn(
                mapOf(
                    cashId to AccountTotals(BigDecimal("100.00"), BigDecimal.ZERO),
                    eqId to AccountTotals(BigDecimal.ZERO, BigDecimal("80.00")),
                ),
            )

        val result = service.getBalanceSheet(orgId, asOf)

        assertThat(result.isBalanced).isFalse()
        assertThat(result.outOfBalanceAmount).isEqualByComparingTo(BigDecimal("20.00"))
    }

    @Test
    fun `getComparativeTrialBalance returns current and comparative`() {
        val asOf = LocalDate.of(2026, 3, 31)
        val compareAsOf = LocalDate.of(2026, 2, 28)
        val currentTb = TrialBalanceResponse(emptyList(), BigDecimal("100.00"), BigDecimal("100.00"), asOf.toString())
        val compareTb =
            TrialBalanceResponse(emptyList(), BigDecimal("80.00"), BigDecimal("80.00"), compareAsOf.toString())

        `when`(journalEntryService.getTrialBalance(orgId, asOf)).thenReturn(currentTb)
        `when`(journalEntryService.getTrialBalance(orgId, compareAsOf)).thenReturn(compareTb)

        val result = service.getComparativeTrialBalance(orgId, asOf, compareAsOf)

        assertThat(result.current).isSameAs(currentTb)
        assertThat(result.comparative).isSameAs(compareTb)
    }

    @Test
    fun `getComparativeTrialBalance omits comparative when not requested`() {
        val asOf = LocalDate.of(2026, 3, 31)
        val currentTb = TrialBalanceResponse(emptyList(), BigDecimal.ZERO, BigDecimal.ZERO, asOf.toString())

        `when`(journalEntryService.getTrialBalance(orgId, asOf)).thenReturn(currentTb)

        val result = service.getComparativeTrialBalance(orgId, asOf, null)

        assertThat(result.comparative).isNull()
    }

    private fun account(
        id: UUID = java.util.UUID.randomUUID(),
        code: String,
        type: AccountType,
    ) = Account(
        id = id,
        code = code,
        name = "Account $code",
        type = type,
        organizationId = orgId,
    )

    private fun entry(
        id: UUID = java.util.UUID.randomUUID(),
        date: LocalDate,
        lines: List<JournalEntryLine>,
    ) = JournalEntry(
        id = id,
        entryNumber = "JE-1",
        date = date,
        description = "test",
        organizationId = orgId,
        status = JournalEntryStatus.POSTED,
        lines = lines,
        createdBy = userId,
    )

    private fun line(
        accountId: UUID,
        debit: BigDecimal,
        credit: BigDecimal,
    ) = JournalEntryLine(
        accountId = accountId,
        accountCode = "1000",
        accountName = "Account Name",
        debit = debit,
        credit = credit,
    )
}
