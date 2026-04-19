package com.froilan.synectix.service

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

    private val orgId = "org-1"
    private val userId = "user-1"

    @BeforeEach
    fun setup() {
        journalEntryRepository = mock(JournalEntryRepository::class.java)
        accountRepository = mock(AccountRepository::class.java)
        service = FinancialReportService(journalEntryRepository, accountRepository)
    }

    @Test
    fun `computePeriodTotals sums debits and credits per account within range`() {
        val start = LocalDate.of(2026, 3, 1)
        val end = LocalDate.of(2026, 3, 31)
        val entries =
            listOf(
                entry(
                    "je-1",
                    LocalDate.of(2026, 3, 5),
                    listOf(
                        line("acc-1", BigDecimal("100.00"), BigDecimal.ZERO),
                        line("acc-2", BigDecimal.ZERO, BigDecimal("100.00")),
                    ),
                ),
                entry(
                    "je-2",
                    LocalDate.of(2026, 3, 20),
                    listOf(
                        line("acc-1", BigDecimal("50.00"), BigDecimal.ZERO),
                        line("acc-2", BigDecimal.ZERO, BigDecimal("50.00")),
                    ),
                ),
            )
        `when`(
            journalEntryRepository.findByOrganizationIdAndStatusInAndDateBetween(
                orgId,
                listOf(JournalEntryStatus.POSTED, JournalEntryStatus.VOIDED),
                start,
                end,
            ),
        ).thenReturn(entries)

        val totals = service.computePeriodTotals(orgId, start, end)

        assertThat(totals["acc-1"]).isEqualTo(BigDecimal("150.00") to BigDecimal.ZERO)
        assertThat(totals["acc-2"]).isEqualTo(BigDecimal.ZERO to BigDecimal("150.00"))
    }

    @Test
    fun `computePeriodTotals uses inception query when startDate is null`() {
        val end = LocalDate.of(2026, 3, 31)
        `when`(
            journalEntryRepository.findByOrganizationIdAndStatusInAndDateLessThanEqual(
                orgId,
                listOf(JournalEntryStatus.POSTED, JournalEntryStatus.VOIDED),
                end,
            ),
        ).thenReturn(emptyList())

        val totals = service.computePeriodTotals(orgId, null, end)

        assertThat(totals).isEmpty()
    }

    @Test
    fun `signedBalance applies normal sides per account type`() {
        val debits = BigDecimal("100.00")
        val credits = BigDecimal("30.00")

        assertThat(service.signedBalance(AccountType.ASSET, debits, credits))
            .isEqualByComparingTo(BigDecimal("70.00"))
        assertThat(service.signedBalance(AccountType.EXPENSE, debits, credits))
            .isEqualByComparingTo(BigDecimal("70.00"))
        assertThat(service.signedBalance(AccountType.LIABILITY, debits, credits))
            .isEqualByComparingTo(BigDecimal("-70.00"))
        assertThat(service.signedBalance(AccountType.EQUITY, debits, credits))
            .isEqualByComparingTo(BigDecimal("-70.00"))
        assertThat(service.signedBalance(AccountType.REVENUE, debits, credits))
            .isEqualByComparingTo(BigDecimal("-70.00"))
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
