package com.froilan.synectix.service

import com.froilan.synectix.dto.CreateFiscalYearRequest
import com.froilan.synectix.model.Account
import com.froilan.synectix.model.AccountType
import com.froilan.synectix.model.FiscalPeriod
import com.froilan.synectix.model.FiscalPeriodStatus
import com.froilan.synectix.model.FiscalYear
import com.froilan.synectix.model.FiscalYearStatus
import com.froilan.synectix.model.JournalEntry
import com.froilan.synectix.model.JournalEntryLine
import com.froilan.synectix.model.JournalEntryStatus
import com.froilan.synectix.repository.AccountRepository
import com.froilan.synectix.repository.FiscalYearRepository
import com.froilan.synectix.repository.JournalEntryRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

class FiscalYearServiceTest {
    private lateinit var fiscalYearService: FiscalYearService
    private lateinit var fiscalYearRepository: FiscalYearRepository
    private lateinit var journalEntryRepository: JournalEntryRepository
    private lateinit var accountRepository: AccountRepository

    private val orgId = "org-123"
    private val userId = "user-1"

    @BeforeEach
    fun setup() {
        fiscalYearRepository = mock(FiscalYearRepository::class.java)
        journalEntryRepository = mock(JournalEntryRepository::class.java)
        accountRepository = mock(AccountRepository::class.java)
        fiscalYearService = FiscalYearService(
            fiscalYearRepository = fiscalYearRepository,
            journalEntryRepository = journalEntryRepository,
            accountRepository = accountRepository,
        )
    }

    @Test
    fun `create should auto-generate 12 monthly periods for calendar year`() {
        `when`(fiscalYearRepository.findByOrganizationId(orgId)).thenReturn(emptyList())
        `when`(fiscalYearRepository.save(any<FiscalYear>())).thenAnswer { it.arguments[0] }

        val request = CreateFiscalYearRequest(
            name = "FY 2026",
            startDate = LocalDate.of(2026, 1, 1),
            endDate = LocalDate.of(2026, 12, 31),
        )

        val result = fiscalYearService.createFiscalYear(request, orgId)

        assertThat(result.name).isEqualTo("FY 2026")
        assertThat(result.status).isEqualTo(FiscalYearStatus.ACTIVE)
        assertThat(result.periods).hasSize(12)
        assertThat(result.periods[0].name).isEqualTo("January 2026")
        assertThat(result.periods[0].startDate).isEqualTo(LocalDate.of(2026, 1, 1))
        assertThat(result.periods[0].endDate).isEqualTo(LocalDate.of(2026, 1, 31))
        assertThat(result.periods[11].name).isEqualTo("December 2026")
        assertThat(result.periods[11].startDate).isEqualTo(LocalDate.of(2026, 12, 1))
        assertThat(result.periods[11].endDate).isEqualTo(LocalDate.of(2026, 12, 31))

        result.periods.forEach { period ->
            assertThat(period.status).isEqualTo(FiscalPeriodStatus.OPEN)
        }
    }

    @Test
    fun `create should handle partial months at boundaries`() {
        `when`(fiscalYearRepository.findByOrganizationId(orgId)).thenReturn(emptyList())
        `when`(fiscalYearRepository.save(any<FiscalYear>())).thenAnswer { it.arguments[0] }

        val request = CreateFiscalYearRequest(
            name = "FY 2026-2027",
            startDate = LocalDate.of(2026, 4, 15),
            endDate = LocalDate.of(2027, 3, 15),
        )

        val result = fiscalYearService.createFiscalYear(request, orgId)

        assertThat(result.periods).hasSize(12)
        assertThat(result.periods[0].startDate).isEqualTo(LocalDate.of(2026, 4, 15))
        assertThat(result.periods[0].endDate).isEqualTo(LocalDate.of(2026, 4, 30))
        assertThat(result.periods.last().endDate).isEqualTo(LocalDate.of(2027, 3, 15))
    }

    @Test
    fun `create should reject if startDate is not before endDate`() {
        val request = CreateFiscalYearRequest(
            name = "FY 2026",
            startDate = LocalDate.of(2026, 12, 31),
            endDate = LocalDate.of(2026, 1, 1),
        )

        val exception = assertThrows<IllegalArgumentException> {
            fiscalYearService.createFiscalYear(request, orgId)
        }
        assertThat(exception.message).contains("Start date must be before end date")
    }

    @Test
    fun `create should reject if dates overlap existing fiscal year`() {
        val existing = createFiscalYear(
            startDate = LocalDate.of(2026, 1, 1),
            endDate = LocalDate.of(2026, 12, 31),
        )
        `when`(fiscalYearRepository.findByOrganizationId(orgId)).thenReturn(listOf(existing))

        val request = CreateFiscalYearRequest(
            name = "FY 2026 Overlap",
            startDate = LocalDate.of(2026, 6, 1),
            endDate = LocalDate.of(2027, 5, 31),
        )

        val exception = assertThrows<IllegalArgumentException> {
            fiscalYearService.createFiscalYear(request, orgId)
        }
        assertThat(exception.message).contains("overlaps")
    }

    @Test
    fun `closePeriod should update period status to CLOSED`() {
        val fiscalYear = createFiscalYear()
        `when`(fiscalYearRepository.findById("fy-1")).thenReturn(Optional.of(fiscalYear))
        `when`(fiscalYearRepository.save(any<FiscalYear>())).thenAnswer { it.arguments[0] }

        val result = fiscalYearService.closePeriod(
            "fy-1",
            fiscalYear.periods[0].id,
            orgId,
            userId,
        )

        assertThat(result.periods[0].status).isEqualTo(FiscalPeriodStatus.CLOSED)
        assertThat(result.periods[0].closedBy).isEqualTo(userId)
        assertThat(result.periods[0].closedAt).isNotNull()
    }

    @Test
    fun `closePeriod should reject if period is already closed`() {
        val fiscalYear = createFiscalYear(
            periods = listOf(
                createPeriod(1, status = FiscalPeriodStatus.CLOSED),
            ),
        )
        `when`(fiscalYearRepository.findById("fy-1")).thenReturn(Optional.of(fiscalYear))

        val exception = assertThrows<IllegalArgumentException> {
            fiscalYearService.closePeriod(
                "fy-1",
                fiscalYear.periods[0].id,
                orgId,
                userId,
            )
        }
        assertThat(exception.message).contains("already closed")
    }

    @Test
    fun `closePeriod should reject if preceding period is still open`() {
        val periods = listOf(
            createPeriod(1, status = FiscalPeriodStatus.OPEN),
            createPeriod(2, status = FiscalPeriodStatus.OPEN),
        )
        val fiscalYear = createFiscalYear(periods = periods)
        `when`(fiscalYearRepository.findById("fy-1")).thenReturn(Optional.of(fiscalYear))

        val exception = assertThrows<IllegalArgumentException> {
            fiscalYearService.closePeriod(
                "fy-1",
                fiscalYear.periods[1].id,
                orgId,
                userId,
            )
        }
        assertThat(exception.message).contains("preceding periods must be closed")
    }

    @Test
    fun `reopenPeriod should set status to REOPENED`() {
        val periods = listOf(
            createPeriod(1, status = FiscalPeriodStatus.CLOSED),
            createPeriod(2, status = FiscalPeriodStatus.CLOSED),
        )
        val fiscalYear = createFiscalYear(periods = periods)
        `when`(fiscalYearRepository.findById("fy-1")).thenReturn(Optional.of(fiscalYear))
        `when`(fiscalYearRepository.save(any<FiscalYear>())).thenAnswer { it.arguments[0] }

        val result = fiscalYearService.reopenPeriod(
            "fy-1",
            fiscalYear.periods[1].id,
            orgId,
            userId,
        )

        assertThat(result.periods[1].status).isEqualTo(FiscalPeriodStatus.REOPENED)
        assertThat(result.periods[1].reopenedBy).isEqualTo(userId)
        assertThat(result.periods[1].reopenedAt).isNotNull()
    }

    @Test
    fun `reopenPeriod should reject if subsequent period is open`() {
        val periods = listOf(
            createPeriod(1, status = FiscalPeriodStatus.CLOSED),
            createPeriod(2, status = FiscalPeriodStatus.OPEN),
        )
        val fiscalYear = createFiscalYear(periods = periods)
        `when`(fiscalYearRepository.findById("fy-1")).thenReturn(Optional.of(fiscalYear))

        val exception = assertThrows<IllegalArgumentException> {
            fiscalYearService.reopenPeriod(
                "fy-1",
                fiscalYear.periods[0].id,
                orgId,
                userId,
            )
        }
        assertThat(exception.message).contains("subsequent periods must be closed")
    }

    @Test
    fun `reopenPeriod should reject if fiscal year is closed`() {
        val periods = listOf(
            createPeriod(1, status = FiscalPeriodStatus.CLOSED),
        )
        val fiscalYear = createFiscalYear(
            periods = periods,
            status = FiscalYearStatus.CLOSED,
        )
        `when`(fiscalYearRepository.findById("fy-1")).thenReturn(Optional.of(fiscalYear))

        val exception = assertThrows<IllegalArgumentException> {
            fiscalYearService.reopenPeriod(
                "fy-1",
                fiscalYear.periods[0].id,
                orgId,
                userId,
            )
        }
        assertThat(exception.message).contains("closed fiscal year")
    }

    @Test
    fun `closeYear should reject if any period is not closed`() {
        val periods = listOf(
            createPeriod(1, status = FiscalPeriodStatus.CLOSED),
            createPeriod(2, status = FiscalPeriodStatus.OPEN),
        )
        val fiscalYear = createFiscalYear(periods = periods)
        `when`(fiscalYearRepository.findById("fy-1")).thenReturn(Optional.of(fiscalYear))

        val exception = assertThrows<IllegalArgumentException> {
            fiscalYearService.closeYear("fy-1", orgId, userId)
        }
        assertThat(exception.message).contains("All periods must be closed")
    }

    @Test
    fun `closeYear should create closing journal entry zeroing revenue and expense`() {
        val periods = listOf(
            createPeriod(1, status = FiscalPeriodStatus.CLOSED),
        )
        val fiscalYear = createFiscalYear(periods = periods)
        `when`(fiscalYearRepository.findById("fy-1")).thenReturn(Optional.of(fiscalYear))
        `when`(fiscalYearRepository.save(any<FiscalYear>())).thenAnswer { it.arguments[0] }

        val revenueAccount = Account(
            id = "acc-revenue",
            code = "4000",
            name = "Sales Revenue",
            type = AccountType.REVENUE,
            organizationId = orgId,
        )
        val expenseAccount = Account(
            id = "acc-expense",
            code = "5000",
            name = "Cost of Goods Sold",
            type = AccountType.EXPENSE,
            organizationId = orgId,
        )
        val retainedEarnings = Account(
            id = "acc-re",
            code = "3100",
            name = "Retained Earnings",
            type = AccountType.EQUITY,
            organizationId = orgId,
        )

        val entries = listOf(
            JournalEntry(
                id = "entry-1",
                entryNumber = "JE-0001",
                date = LocalDate.of(2026, 1, 15),
                description = "Sale",
                organizationId = orgId,
                status = JournalEntryStatus.POSTED,
                lines = listOf(
                    JournalEntryLine(
                        accountId = "acc-cash",
                        accountCode = "1000",
                        accountName = "Cash",
                        debit = BigDecimal("1000.00"),
                        credit = BigDecimal.ZERO,
                    ),
                    JournalEntryLine(
                        accountId = "acc-revenue",
                        accountCode = "4000",
                        accountName = "Sales Revenue",
                        debit = BigDecimal.ZERO,
                        credit = BigDecimal("1000.00"),
                    ),
                ),
                createdBy = userId,
            ),
            JournalEntry(
                id = "entry-2",
                entryNumber = "JE-0002",
                date = LocalDate.of(2026, 1, 20),
                description = "Purchase",
                organizationId = orgId,
                status = JournalEntryStatus.POSTED,
                lines = listOf(
                    JournalEntryLine(
                        accountId = "acc-expense",
                        accountCode = "5000",
                        accountName = "Cost of Goods Sold",
                        debit = BigDecimal("300.00"),
                        credit = BigDecimal.ZERO,
                    ),
                    JournalEntryLine(
                        accountId = "acc-cash",
                        accountCode = "1000",
                        accountName = "Cash",
                        debit = BigDecimal.ZERO,
                        credit = BigDecimal("300.00"),
                    ),
                ),
                createdBy = userId,
            ),
        )

        `when`(
            journalEntryRepository.findByOrganizationIdAndStatusAndDateBetween(
                orgId,
                JournalEntryStatus.POSTED,
                fiscalYear.startDate,
                fiscalYear.endDate,
            ),
        ).thenReturn(entries)

        `when`(accountRepository.findByOrganizationIdAndIsActive(orgId, true))
            .thenReturn(listOf(revenueAccount, expenseAccount, retainedEarnings))
        `when`(accountRepository.findByOrganizationIdAndCode(orgId, "3100"))
            .thenReturn(Optional.of(retainedEarnings))
        `when`(journalEntryRepository.countByOrganizationId(orgId)).thenReturn(2L)
        `when`(journalEntryRepository.save(any<JournalEntry>())).thenAnswer { it.arguments[0] }

        val result = fiscalYearService.closeYear("fy-1", orgId, userId)

        assertThat(result.status).isEqualTo(FiscalYearStatus.CLOSED)
        assertThat(result.closedBy).isEqualTo(userId)
        assertThat(result.closedAt).isNotNull()
        assertThat(result.closingEntryId).isNotNull()

        val entryCaptor = argumentCaptor<JournalEntry>()
        verify(journalEntryRepository).save(entryCaptor.capture())

        val closingEntry = entryCaptor.firstValue
        assertThat(closingEntry.description).contains("Year-end closing entry")
        assertThat(closingEntry.status).isEqualTo(JournalEntryStatus.POSTED)

        // Revenue (1000 credit balance) should be debited to zero it
        val revenueLine = closingEntry.lines.find { it.accountId == "acc-revenue" }
        assertThat(revenueLine).isNotNull
        assertThat(revenueLine!!.debit).isEqualByComparingTo(BigDecimal("1000.00"))

        // Expense (300 debit balance) should be credited to zero it
        val expenseLine = closingEntry.lines.find { it.accountId == "acc-expense" }
        assertThat(expenseLine).isNotNull
        assertThat(expenseLine!!.credit).isEqualByComparingTo(BigDecimal("300.00"))

        // Net income = 1000 - 300 = 700, credited to Retained Earnings
        val reLine = closingEntry.lines.find { it.accountId == "acc-re" }
        assertThat(reLine).isNotNull
        assertThat(reLine!!.credit).isEqualByComparingTo(BigDecimal("700.00"))
    }

    @Test
    fun `closeYear should handle net loss correctly`() {
        val periods = listOf(
            createPeriod(1, status = FiscalPeriodStatus.CLOSED),
        )
        val fiscalYear = createFiscalYear(periods = periods)
        `when`(fiscalYearRepository.findById("fy-1")).thenReturn(Optional.of(fiscalYear))
        `when`(fiscalYearRepository.save(any<FiscalYear>())).thenAnswer { it.arguments[0] }

        val expenseAccount = Account(
            id = "acc-expense",
            code = "5000",
            name = "Cost of Goods Sold",
            type = AccountType.EXPENSE,
            organizationId = orgId,
        )
        val retainedEarnings = Account(
            id = "acc-re",
            code = "3100",
            name = "Retained Earnings",
            type = AccountType.EQUITY,
            organizationId = orgId,
        )

        val entries = listOf(
            JournalEntry(
                id = "entry-1",
                entryNumber = "JE-0001",
                date = LocalDate.of(2026, 1, 15),
                description = "Expense only",
                organizationId = orgId,
                status = JournalEntryStatus.POSTED,
                lines = listOf(
                    JournalEntryLine(
                        accountId = "acc-expense",
                        accountCode = "5000",
                        accountName = "Cost of Goods Sold",
                        debit = BigDecimal("500.00"),
                        credit = BigDecimal.ZERO,
                    ),
                    JournalEntryLine(
                        accountId = "acc-cash",
                        accountCode = "1000",
                        accountName = "Cash",
                        debit = BigDecimal.ZERO,
                        credit = BigDecimal("500.00"),
                    ),
                ),
                createdBy = userId,
            ),
        )

        `when`(
            journalEntryRepository.findByOrganizationIdAndStatusAndDateBetween(
                orgId,
                JournalEntryStatus.POSTED,
                fiscalYear.startDate,
                fiscalYear.endDate,
            ),
        ).thenReturn(entries)

        `when`(accountRepository.findByOrganizationIdAndIsActive(orgId, true))
            .thenReturn(listOf(expenseAccount, retainedEarnings))
        `when`(accountRepository.findByOrganizationIdAndCode(orgId, "3100"))
            .thenReturn(Optional.of(retainedEarnings))
        `when`(journalEntryRepository.countByOrganizationId(orgId)).thenReturn(1L)
        `when`(journalEntryRepository.save(any<JournalEntry>())).thenAnswer { it.arguments[0] }

        val result = fiscalYearService.closeYear("fy-1", orgId, userId)

        assertThat(result.status).isEqualTo(FiscalYearStatus.CLOSED)

        val entryCaptor = argumentCaptor<JournalEntry>()
        verify(journalEntryRepository).save(entryCaptor.capture())

        val closingEntry = entryCaptor.firstValue
        // Net loss: 0 revenue - 500 expense = -500
        // Retained Earnings should be debited (reducing equity)
        val reLine = closingEntry.lines.find { it.accountId == "acc-re" }
        assertThat(reLine).isNotNull
        assertThat(reLine!!.debit).isEqualByComparingTo(BigDecimal("500.00"))
        assertThat(reLine.credit).isEqualByComparingTo(BigDecimal.ZERO)
    }

    @Test
    fun `closeYear should skip closing entry when no revenue or expense activity`() {
        val periods = listOf(
            createPeriod(1, status = FiscalPeriodStatus.CLOSED),
        )
        val fiscalYear = createFiscalYear(periods = periods)
        `when`(fiscalYearRepository.findById("fy-1")).thenReturn(Optional.of(fiscalYear))
        `when`(fiscalYearRepository.save(any<FiscalYear>())).thenAnswer { it.arguments[0] }

        `when`(
            journalEntryRepository.findByOrganizationIdAndStatusAndDateBetween(
                orgId,
                JournalEntryStatus.POSTED,
                fiscalYear.startDate,
                fiscalYear.endDate,
            ),
        ).thenReturn(emptyList())

        `when`(accountRepository.findByOrganizationIdAndIsActive(orgId, true))
            .thenReturn(emptyList())

        val result = fiscalYearService.closeYear("fy-1", orgId, userId)

        assertThat(result.status).isEqualTo(FiscalYearStatus.CLOSED)
        assertThat(result.closingEntryId).isNull()
        verify(journalEntryRepository, never()).save(any<JournalEntry>())
    }

    @Test
    fun `findOpenPeriodForDate should return matching open period`() {
        val fiscalYear = createFiscalYear()
        `when`(
            fiscalYearRepository.findByOrganizationIdAndStatus(
                orgId,
                FiscalYearStatus.ACTIVE,
            ),
        ).thenReturn(listOf(fiscalYear))

        val result = fiscalYearService.findOpenPeriodForDate(
            orgId,
            LocalDate.of(2026, 1, 15),
        )

        assertThat(result).isNotNull
        assertThat(result!!.status).isEqualTo(FiscalPeriodStatus.OPEN)
    }

    @Test
    fun `findOpenPeriodForDate should return null when no fiscal year exists`() {
        `when`(
            fiscalYearRepository.findByOrganizationIdAndStatus(
                orgId,
                FiscalYearStatus.ACTIVE,
            ),
        ).thenReturn(emptyList())

        val result = fiscalYearService.findOpenPeriodForDate(
            orgId,
            LocalDate.of(2026, 1, 15),
        )

        assertThat(result).isNull()
    }

    @Test
    fun `findOpenPeriodForDate should return null when date is outside fiscal year`() {
        val fiscalYear = createFiscalYear()
        `when`(
            fiscalYearRepository.findByOrganizationIdAndStatus(
                orgId,
                FiscalYearStatus.ACTIVE,
            ),
        ).thenReturn(listOf(fiscalYear))

        val result = fiscalYearService.findOpenPeriodForDate(
            orgId,
            LocalDate.of(2025, 6, 15),
        )

        assertThat(result).isNull()
    }

    @Test
    fun `findOpenPeriodForDate should return reopened period`() {
        val periods = listOf(
            createPeriod(1, status = FiscalPeriodStatus.REOPENED),
        )
        val fiscalYear = createFiscalYear(periods = periods)
        `when`(
            fiscalYearRepository.findByOrganizationIdAndStatus(
                orgId,
                FiscalYearStatus.ACTIVE,
            ),
        ).thenReturn(listOf(fiscalYear))

        val result = fiscalYearService.findOpenPeriodForDate(
            orgId,
            LocalDate.of(2026, 1, 15),
        )

        assertThat(result).isNotNull
        assertThat(result!!.status).isEqualTo(FiscalPeriodStatus.REOPENED)
    }

    private fun createFiscalYear(
        id: String = "fy-1",
        name: String = "FY 2026",
        startDate: LocalDate = LocalDate.of(2026, 1, 1),
        endDate: LocalDate = LocalDate.of(2026, 12, 31),
        status: FiscalYearStatus = FiscalYearStatus.ACTIVE,
        periods: List<FiscalPeriod>? = null,
    ): FiscalYear = FiscalYear(
        id = id,
        name = name,
        startDate = startDate,
        endDate = endDate,
        status = status,
        organizationId = orgId,
        periods = periods ?: listOf(
            createPeriod(1),
        ),
    )

    private fun createPeriod(
        number: Int,
        status: FiscalPeriodStatus = FiscalPeriodStatus.OPEN,
    ): FiscalPeriod = FiscalPeriod(
        periodNumber = number,
        name = "January 2026",
        startDate = LocalDate.of(2026, 1, 1),
        endDate = LocalDate.of(2026, 1, 31),
        status = status,
    )
}
