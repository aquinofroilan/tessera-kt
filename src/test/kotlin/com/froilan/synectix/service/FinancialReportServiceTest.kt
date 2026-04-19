package com.froilan.synectix.service

import com.froilan.synectix.dto.SyntheticAccountIds
import com.froilan.synectix.dto.TrialBalanceResponse
import com.froilan.synectix.exception.BusinessRuleException
import com.froilan.synectix.model.Account
import com.froilan.synectix.model.AccountType
import com.froilan.synectix.model.JournalEntry
import com.froilan.synectix.model.JournalEntryLine
import com.froilan.synectix.model.JournalEntryStatus
import com.froilan.synectix.repository.AccountRepository
import com.froilan.synectix.repository.JournalEntryRepository
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

    private val orgId = "org-1"
    private val userId = "user-1"

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
        val revenue = account("acc-rev", "4100", AccountType.REVENUE)
        val expense = account("acc-exp", "5000", AccountType.EXPENSE)

        `when`(accountRepository.findByOrganizationIdAndIsActive(orgId, true))
            .thenReturn(listOf(expense, revenue))
        `when`(
            journalEntryRepository.findByOrganizationIdAndStatusInAndDateBetween(
                orgId,
                listOf(JournalEntryStatus.POSTED, JournalEntryStatus.VOIDED),
                start,
                end,
            ),
        ).thenReturn(
            listOf(
                entry(
                    "je-1",
                    LocalDate.of(2026, 3, 10),
                    listOf(
                        line("acc-rev", BigDecimal.ZERO, BigDecimal("1000.00")),
                        line("acc-exp", BigDecimal("300.00"), BigDecimal.ZERO),
                    ),
                ),
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
        val revenue = account("acc-rev", "4100", AccountType.REVENUE)

        `when`(accountRepository.findByOrganizationIdAndIsActive(orgId, true))
            .thenReturn(listOf(revenue))
        `when`(
            journalEntryRepository.findByOrganizationIdAndStatusInAndDateBetween(
                orgId,
                listOf(JournalEntryStatus.POSTED, JournalEntryStatus.VOIDED),
                start,
                end,
            ),
        ).thenReturn(
            listOf(
                entry(
                    "je-1",
                    LocalDate.of(2026, 3, 10),
                    listOf(line("acc-rev", BigDecimal.ZERO, BigDecimal("1000.00"))),
                ),
            ),
        )
        `when`(
            journalEntryRepository.findByOrganizationIdAndStatusInAndDateBetween(
                orgId,
                listOf(JournalEntryStatus.POSTED, JournalEntryStatus.VOIDED),
                compareStart,
                compareEnd,
            ),
        ).thenReturn(
            listOf(
                entry(
                    "je-2",
                    LocalDate.of(2026, 2, 15),
                    listOf(line("acc-rev", BigDecimal.ZERO, BigDecimal("800.00"))),
                ),
            ),
        )

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
        `when`(accountRepository.findByOrganizationIdAndIsActive(orgId, true))
            .thenReturn(listOf(account("acc-cash", "1000", AccountType.ASSET)))
        `when`(
            journalEntryRepository.findByOrganizationIdAndStatusInAndDateBetween(
                orgId,
                listOf(JournalEntryStatus.POSTED, JournalEntryStatus.VOIDED),
                start,
                end,
            ),
        ).thenReturn(emptyList())

        val result = service.getIncomeStatement(orgId, start, end)

        assertThat(result.revenue).isEmpty()
        assertThat(result.expenses).isEmpty()
        assertThat(result.netIncome).isEqualByComparingTo(BigDecimal.ZERO)
    }

    @Test
    fun `getBalanceSheet computes totals and current earnings`() {
        val asOf = LocalDate.of(2026, 3, 31)
        val cash = account("acc-cash", "1000", AccountType.ASSET)
        val ap = account("acc-ap", "2000", AccountType.LIABILITY)
        val equity = account("acc-eq", "3000", AccountType.EQUITY)
        val rev = account("acc-rev", "4000", AccountType.REVENUE)
        val exp = account("acc-exp", "5000", AccountType.EXPENSE)

        `when`(accountRepository.findByOrganizationIdAndIsActive(orgId, true))
            .thenReturn(listOf(cash, ap, equity, rev, exp))
        `when`(
            journalEntryRepository.findByOrganizationIdAndStatusInAndDateLessThanEqual(
                orgId,
                listOf(JournalEntryStatus.POSTED, JournalEntryStatus.VOIDED),
                asOf,
            ),
        ).thenReturn(
            listOf(
                entry(
                    "je-1",
                    LocalDate.of(2026, 1, 1),
                    listOf(
                        line("acc-cash", BigDecimal("10000.00"), BigDecimal.ZERO),
                        line("acc-eq", BigDecimal.ZERO, BigDecimal("10000.00")),
                    ),
                ),
                entry(
                    "je-2",
                    LocalDate.of(2026, 3, 5),
                    listOf(
                        line("acc-cash", BigDecimal("3000.00"), BigDecimal.ZERO),
                        line("acc-rev", BigDecimal.ZERO, BigDecimal("3000.00")),
                    ),
                ),
                entry(
                    "je-3",
                    LocalDate.of(2026, 3, 10),
                    listOf(
                        line("acc-exp", BigDecimal("500.00"), BigDecimal.ZERO),
                        line("acc-cash", BigDecimal.ZERO, BigDecimal("500.00")),
                    ),
                ),
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
        assertThat(earningsRow.accountId).isEqualTo(SyntheticAccountIds.CURRENT_PERIOD_EARNINGS)
        assertThat(earningsRow.accountCode).isEqualTo(SyntheticAccountIds.CURRENT_PERIOD_EARNINGS)
        assertThat(earningsRow.isSynthetic).isTrue()
    }

    @Test
    fun `getBalanceSheet flags imbalance without throwing`() {
        val asOf = LocalDate.of(2026, 3, 31)
        val cash = account("acc-cash", "1000", AccountType.ASSET)
        val equity = account("acc-eq", "3000", AccountType.EQUITY)

        `when`(accountRepository.findByOrganizationIdAndIsActive(orgId, true))
            .thenReturn(listOf(cash, equity))
        `when`(
            journalEntryRepository.findByOrganizationIdAndStatusInAndDateLessThanEqual(
                orgId,
                listOf(JournalEntryStatus.POSTED, JournalEntryStatus.VOIDED),
                asOf,
            ),
        ).thenReturn(
            listOf(
                entry(
                    "je-1",
                    LocalDate.of(2026, 1, 1),
                    listOf(
                        line("acc-cash", BigDecimal("100.00"), BigDecimal.ZERO),
                        line("acc-eq", BigDecimal.ZERO, BigDecimal("80.00")),
                    ),
                ),
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

    private fun entry(
        id: String,
        date: LocalDate,
        lines: List<JournalEntryLine>,
    ) = JournalEntry(
        id = id,
        entryNumber = id.uppercase(),
        date = date,
        description = "test",
        organizationId = orgId,
        status = JournalEntryStatus.POSTED,
        lines = lines,
        createdBy = userId,
    )

    private fun line(
        accountId: String,
        debit: BigDecimal,
        credit: BigDecimal,
    ) = JournalEntryLine(
        accountId = accountId,
        accountCode = accountId,
        accountName = "Account $accountId",
        debit = debit,
        credit = credit,
    )
}
