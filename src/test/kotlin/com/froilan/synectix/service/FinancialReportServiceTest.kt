package com.froilan.synectix.service

import com.froilan.synectix.model.AccountType
import com.froilan.synectix.model.JournalEntry
import com.froilan.synectix.model.JournalEntryLine
import com.froilan.synectix.model.JournalEntryStatus
import com.froilan.synectix.repository.JournalEntryRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.LocalDate

class FinancialReportServiceTest {
    private lateinit var service: FinancialReportService
    private lateinit var journalEntryRepository: JournalEntryRepository

    private val orgId = "org-1"
    private val userId = "user-1"

    @BeforeEach
    fun setup() {
        journalEntryRepository = mock(JournalEntryRepository::class.java)
        service = FinancialReportService(journalEntryRepository)
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
