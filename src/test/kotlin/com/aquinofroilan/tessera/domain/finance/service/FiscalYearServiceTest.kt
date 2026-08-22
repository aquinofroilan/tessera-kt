package com.aquinofroilan.tessera.domain.finance.service

import com.aquinofroilan.tessera.domain.finance.model.Account
import com.aquinofroilan.tessera.domain.finance.model.AccountType
import com.aquinofroilan.tessera.domain.finance.model.FiscalPeriod
import com.aquinofroilan.tessera.domain.finance.model.FiscalPeriodStatus
import com.aquinofroilan.tessera.domain.finance.model.FiscalYear
import com.aquinofroilan.tessera.domain.finance.model.FiscalYearStatus
import com.aquinofroilan.tessera.domain.finance.model.JournalEntry
import com.aquinofroilan.tessera.domain.finance.model.JournalEntryLine
import com.aquinofroilan.tessera.domain.finance.model.JournalEntryStatus
import com.aquinofroilan.tessera.domain.finance.repository.AccountRepository
import com.aquinofroilan.tessera.domain.finance.repository.FiscalYearRepository
import com.aquinofroilan.tessera.domain.finance.repository.JournalEntryRepository
import com.aquinofroilan.tessera.domain.platform.dto.CreateFiscalYearRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
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
    private lateinit var entryNumberGenerator: JournalEntryNumberGenerator

    private val orgId = java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d")
    private val userId = java.util.UUID.fromString("1db2395f-13ba-3d37-9d2b-f77d3eb3aa2e")

    @BeforeEach
    fun setup() {
        fiscalYearRepository = mock(FiscalYearRepository::class.java)
        journalEntryRepository = mock(JournalEntryRepository::class.java)
        accountRepository = mock(AccountRepository::class.java)
        entryNumberGenerator = JournalEntryNumberGenerator(journalEntryRepository)
        fiscalYearService =
            FiscalYearService(
                fiscalYearRepository = fiscalYearRepository,
                journalEntryRepository = journalEntryRepository,
                accountRepository = accountRepository,
                entryNumberGenerator = entryNumberGenerator,
            )
    }

    @Test
    fun `create should auto-generate 12 monthly periods for calendar year`() {
        `when`(fiscalYearRepository.findByOrganizationId(orgId)).thenReturn(emptyList())
        `when`(fiscalYearRepository.save(any<FiscalYear>())).thenAnswer { it.arguments[0] }

        val request =
            CreateFiscalYearRequest(
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

        val request =
            CreateFiscalYearRequest(
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
        val request =
            CreateFiscalYearRequest(
                name = "FY 2026",
                startDate = LocalDate.of(2026, 12, 31),
                endDate = LocalDate.of(2026, 1, 1),
            )

        val exception =
            assertThrows<BusinessRuleException> {
                fiscalYearService.createFiscalYear(request, orgId)
            }
        assertThat(exception.message).contains("Start date must be before end date")
    }

    @Test
    fun `create should reject if active fiscal year already exists`() {
        val existing =
            createFiscalYear(
                startDate = LocalDate.of(2026, 1, 1),
                endDate = LocalDate.of(2026, 12, 31),
                status = FiscalYearStatus.ACTIVE,
            )
        `when`(fiscalYearRepository.findByOrganizationId(orgId)).thenReturn(listOf(existing))

        val request =
            CreateFiscalYearRequest(
                name = "FY 2027",
                startDate = LocalDate.of(2027, 1, 1),
                endDate = LocalDate.of(2027, 12, 31),
            )

        val exception =
            assertThrows<BusinessRuleException> {
                fiscalYearService.createFiscalYear(request, orgId)
            }
        assertThat(exception.message).contains("active fiscal year")
    }

    @Test
    fun `create should reject if dates overlap existing fiscal year`() {
        val existing =
            createFiscalYear(
                startDate = LocalDate.of(2026, 1, 1),
                endDate = LocalDate.of(2026, 12, 31),
                status = FiscalYearStatus.CLOSED,
            )
        `when`(fiscalYearRepository.findByOrganizationId(orgId)).thenReturn(listOf(existing))

        val request =
            CreateFiscalYearRequest(
                name = "FY 2026 Overlap",
                startDate = LocalDate.of(2026, 6, 1),
                endDate = LocalDate.of(2027, 5, 31),
            )

        val exception =
            assertThrows<BusinessRuleException> {
                fiscalYearService.createFiscalYear(request, orgId)
            }
        assertThat(exception.message).contains("overlaps")
    }

    @Test
    fun `closePeriod should update period status to CLOSED`() {
        val fiscalYear = createFiscalYear()
        `when`(
            fiscalYearRepository.findById(java.util.UUID.fromString("e70d185f-67cf-3457-bb30-61da0a63102e")),
        ).thenReturn(Optional.of(fiscalYear))
        `when`(fiscalYearRepository.save(any<FiscalYear>())).thenAnswer { it.arguments[0] }

        val result =
            fiscalYearService.closePeriod(
                java.util.UUID.fromString("e70d185f-67cf-3457-bb30-61da0a63102e"),
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
        val fiscalYear =
            createFiscalYear(
                periods =
                    listOf(
                        createPeriod(1, status = FiscalPeriodStatus.CLOSED),
                    ),
            )
        `when`(
            fiscalYearRepository.findById(java.util.UUID.fromString("e70d185f-67cf-3457-bb30-61da0a63102e")),
        ).thenReturn(Optional.of(fiscalYear))

        val exception =
            assertThrows<BusinessRuleException> {
                fiscalYearService.closePeriod(
                    java.util.UUID.fromString("e70d185f-67cf-3457-bb30-61da0a63102e"),
                    fiscalYear.periods[0].id,
                    orgId,
                    userId,
                )
            }
        assertThat(exception.message).contains("already closed")
    }

    @Test
    fun `closePeriod should reject if preceding period is still open`() {
        val periods =
            listOf(
                createPeriod(1, status = FiscalPeriodStatus.OPEN),
                createPeriod(2, status = FiscalPeriodStatus.OPEN),
            )
        val fiscalYear = createFiscalYear(periods = periods)
        `when`(
            fiscalYearRepository.findById(java.util.UUID.fromString("e70d185f-67cf-3457-bb30-61da0a63102e")),
        ).thenReturn(Optional.of(fiscalYear))

        val exception =
            assertThrows<BusinessRuleException> {
                fiscalYearService.closePeriod(
                    java.util.UUID.fromString("e70d185f-67cf-3457-bb30-61da0a63102e"),
                    fiscalYear.periods[1].id,
                    orgId,
                    userId,
                )
            }
        assertThat(exception.message).contains("preceding periods must be closed")
    }

    @Test
    fun `reopenPeriod should set status to REOPENED`() {
        val periods =
            listOf(
                createPeriod(1, status = FiscalPeriodStatus.CLOSED),
                createPeriod(2, status = FiscalPeriodStatus.CLOSED),
            )
        val fiscalYear = createFiscalYear(periods = periods)
        `when`(
            fiscalYearRepository.findById(java.util.UUID.fromString("e70d185f-67cf-3457-bb30-61da0a63102e")),
        ).thenReturn(Optional.of(fiscalYear))
        `when`(fiscalYearRepository.save(any<FiscalYear>())).thenAnswer { it.arguments[0] }

        val result =
            fiscalYearService.reopenPeriod(
                java.util.UUID.fromString("e70d185f-67cf-3457-bb30-61da0a63102e"),
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
        val periods =
            listOf(
                createPeriod(1, status = FiscalPeriodStatus.CLOSED),
                createPeriod(2, status = FiscalPeriodStatus.OPEN),
            )
        val fiscalYear = createFiscalYear(periods = periods)
        `when`(
            fiscalYearRepository.findById(java.util.UUID.fromString("e70d185f-67cf-3457-bb30-61da0a63102e")),
        ).thenReturn(Optional.of(fiscalYear))

        val exception =
            assertThrows<BusinessRuleException> {
                fiscalYearService.reopenPeriod(
                    java.util.UUID.fromString("e70d185f-67cf-3457-bb30-61da0a63102e"),
                    fiscalYear.periods[0].id,
                    orgId,
                    userId,
                )
            }
        assertThat(exception.message).contains("subsequent periods must be closed")
    }

    @Test
    fun `reopenPeriod should reject if fiscal year is closed`() {
        val periods =
            listOf(
                createPeriod(1, status = FiscalPeriodStatus.CLOSED),
            )
        val fiscalYear =
            createFiscalYear(
                periods = periods,
                status = FiscalYearStatus.CLOSED,
            )
        `when`(
            fiscalYearRepository.findById(java.util.UUID.fromString("e70d185f-67cf-3457-bb30-61da0a63102e")),
        ).thenReturn(Optional.of(fiscalYear))

        val exception =
            assertThrows<BusinessRuleException> {
                fiscalYearService.reopenPeriod(
                    java.util.UUID.fromString("e70d185f-67cf-3457-bb30-61da0a63102e"),
                    fiscalYear.periods[0].id,
                    orgId,
                    userId,
                )
            }
        assertThat(exception.message).contains("closed fiscal year")
    }

    @Test
    fun `closeYear should reject if any period is not closed`() {
        val periods =
            listOf(
                createPeriod(1, status = FiscalPeriodStatus.CLOSED),
                createPeriod(2, status = FiscalPeriodStatus.OPEN),
            )
        val fiscalYear = createFiscalYear(periods = periods)
        `when`(
            fiscalYearRepository.findById(java.util.UUID.fromString("e70d185f-67cf-3457-bb30-61da0a63102e")),
        ).thenReturn(Optional.of(fiscalYear))

        val exception =
            assertThrows<BusinessRuleException> {
                fiscalYearService.closeYear(java.util.UUID.fromString("e70d185f-67cf-3457-bb30-61da0a63102e"), orgId, userId)
            }
        assertThat(exception.message).contains("All periods must be closed")
    }

    @Test
    fun `closeYear should create closing journal entry zeroing revenue and expense`() {
        val periods =
            listOf(
                createPeriod(1, status = FiscalPeriodStatus.CLOSED),
            )
        val fiscalYear = createFiscalYear(periods = periods)
        `when`(
            fiscalYearRepository.findById(java.util.UUID.fromString("e70d185f-67cf-3457-bb30-61da0a63102e")),
        ).thenReturn(Optional.of(fiscalYear))
        `when`(fiscalYearRepository.save(any<FiscalYear>())).thenAnswer { it.arguments[0] }
        `when`(journalEntryRepository.existsByOrganizationIdAndSourceReference(orgId, "YEAR-END-CLOSE-fy-1"))
            .thenReturn(false)

        val cashAccount =
            Account(
                id = java.util.UUID.fromString("76aee8a0-be1c-3046-ac38-0c0b819d0b1e"),
                code = "1000",
                name = "Cash",
                type = AccountType.ASSET,
                organizationId = orgId,
            )
        val revenueAccount =
            Account(
                id = java.util.UUID.fromString("56bc042f-5295-31d7-9573-57ec543647ff"),
                code = "4000",
                name = "Sales Revenue",
                type = AccountType.REVENUE,
                organizationId = orgId,
            )
        val expenseAccount =
            Account(
                id = java.util.UUID.fromString("ae4979aa-053c-3d3b-98ce-8698038738d3"),
                code = "5000",
                name = "Cost of Goods Sold",
                type = AccountType.EXPENSE,
                organizationId = orgId,
            )
        val retainedEarnings =
            Account(
                id = java.util.UUID.fromString("92b6a7ae-9ab7-31f4-a3cc-05d7399fecac"),
                code = "3100",
                name = "Retained Earnings",
                type = AccountType.EQUITY,
                organizationId = orgId,
            )

        val entries =
            listOf(
                JournalEntry(
                    id = java.util.UUID.fromString("883cd44c-a464-3674-a0ec-1fb21a7ccd22"),
                    entryNumber = "JE-0001",
                    date = LocalDate.of(2026, 1, 15),
                    description = "Sale",
                    organizationId = orgId,
                    status = JournalEntryStatus.POSTED,
                    lines =
                        listOf(
                            JournalEntryLine(
                                accountId = java.util.UUID.fromString("76aee8a0-be1c-3046-ac38-0c0b819d0b1e"),
                                accountCode = "1000",
                                accountName = "Cash",
                                debit = BigDecimal("1000.00"),
                                credit = BigDecimal.ZERO,
                            ),
                            JournalEntryLine(
                                accountId = java.util.UUID.fromString("56bc042f-5295-31d7-9573-57ec543647ff"),
                                accountCode = "4000",
                                accountName = "Sales Revenue",
                                debit = BigDecimal.ZERO,
                                credit = BigDecimal("1000.00"),
                            ),
                        ),
                    createdBy = userId,
                ),
                JournalEntry(
                    id = java.util.UUID.fromString("f9029c57-059a-3b7c-8aaf-2dfc79036689"),
                    entryNumber = "JE-0002",
                    date = LocalDate.of(2026, 1, 20),
                    description = "Purchase",
                    organizationId = orgId,
                    status = JournalEntryStatus.POSTED,
                    lines =
                        listOf(
                            JournalEntryLine(
                                accountId = java.util.UUID.fromString("ae4979aa-053c-3d3b-98ce-8698038738d3"),
                                accountCode = "5000",
                                accountName = "Cost of Goods Sold",
                                debit = BigDecimal("300.00"),
                                credit = BigDecimal.ZERO,
                            ),
                            JournalEntryLine(
                                accountId = java.util.UUID.fromString("76aee8a0-be1c-3046-ac38-0c0b819d0b1e"),
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
            journalEntryRepository.findByOrganizationIdAndStatusInAndDateBetween(
                orgId,
                listOf(JournalEntryStatus.POSTED, JournalEntryStatus.VOIDED),
                fiscalYear.startDate,
                fiscalYear.endDate,
            ),
        ).thenReturn(entries)

        `when`(accountRepository.findAllById(any<Iterable<java.util.UUID>>()))
            .thenReturn(listOf(cashAccount, revenueAccount, expenseAccount, retainedEarnings))
        `when`(accountRepository.findByOrganizationIdAndCode(orgId, "3100"))
            .thenReturn(Optional.of(retainedEarnings))
        `when`(journalEntryRepository.countByOrganizationId(orgId)).thenReturn(2L)
        `when`(journalEntryRepository.save(any<JournalEntry>())).thenAnswer { it.arguments[0] }

        val result = fiscalYearService.closeYear(java.util.UUID.fromString("e70d185f-67cf-3457-bb30-61da0a63102e"), orgId, userId)

        assertThat(result.status).isEqualTo(FiscalYearStatus.CLOSED)
        assertThat(result.closedBy).isEqualTo(userId)
        assertThat(result.closedAt).isNotNull()
        assertThat(result.closingEntryId).isNotNull()

        val entryCaptor = argumentCaptor<JournalEntry>()
        verify(journalEntryRepository).save(entryCaptor.capture())

        val closingEntry = entryCaptor.firstValue
        assertThat(closingEntry.description).contains("Year-end closing entry")
        assertThat(closingEntry.status).isEqualTo(JournalEntryStatus.POSTED)

        val revenueLine = closingEntry.lines.find { it.accountId == java.util.UUID.fromString("56bc042f-5295-31d7-9573-57ec543647ff") }
        assertThat(revenueLine).isNotNull
        assertThat(revenueLine!!.debit).isEqualByComparingTo(BigDecimal("1000.00"))

        val expenseLine = closingEntry.lines.find { it.accountId == java.util.UUID.fromString("ae4979aa-053c-3d3b-98ce-8698038738d3") }
        assertThat(expenseLine).isNotNull
        assertThat(expenseLine!!.credit).isEqualByComparingTo(BigDecimal("300.00"))

        val reLine = closingEntry.lines.find { it.accountId == java.util.UUID.fromString("92b6a7ae-9ab7-31f4-a3cc-05d7399fecac") }
        assertThat(reLine).isNotNull
        assertThat(reLine!!.credit).isEqualByComparingTo(BigDecimal("700.00"))
    }

    @Test
    fun `closeYear should handle net loss correctly`() {
        val periods =
            listOf(
                createPeriod(1, status = FiscalPeriodStatus.CLOSED),
            )
        val fiscalYear = createFiscalYear(periods = periods)
        `when`(
            fiscalYearRepository.findById(java.util.UUID.fromString("e70d185f-67cf-3457-bb30-61da0a63102e")),
        ).thenReturn(Optional.of(fiscalYear))
        `when`(fiscalYearRepository.save(any<FiscalYear>())).thenAnswer { it.arguments[0] }
        `when`(journalEntryRepository.existsByOrganizationIdAndSourceReference(orgId, "YEAR-END-CLOSE-fy-1"))
            .thenReturn(false)

        val cashAccount =
            Account(
                id = java.util.UUID.fromString("76aee8a0-be1c-3046-ac38-0c0b819d0b1e"),
                code = "1000",
                name = "Cash",
                type = AccountType.ASSET,
                organizationId = orgId,
            )
        val expenseAccount =
            Account(
                id = java.util.UUID.fromString("ae4979aa-053c-3d3b-98ce-8698038738d3"),
                code = "5000",
                name = "Cost of Goods Sold",
                type = AccountType.EXPENSE,
                organizationId = orgId,
            )
        val retainedEarnings =
            Account(
                id = java.util.UUID.fromString("92b6a7ae-9ab7-31f4-a3cc-05d7399fecac"),
                code = "3100",
                name = "Retained Earnings",
                type = AccountType.EQUITY,
                organizationId = orgId,
            )

        val entries =
            listOf(
                JournalEntry(
                    id = java.util.UUID.fromString("883cd44c-a464-3674-a0ec-1fb21a7ccd22"),
                    entryNumber = "JE-0001",
                    date = LocalDate.of(2026, 1, 15),
                    description = "Expense only",
                    organizationId = orgId,
                    status = JournalEntryStatus.POSTED,
                    lines =
                        listOf(
                            JournalEntryLine(
                                accountId = java.util.UUID.fromString("ae4979aa-053c-3d3b-98ce-8698038738d3"),
                                accountCode = "5000",
                                accountName = "Cost of Goods Sold",
                                debit = BigDecimal("500.00"),
                                credit = BigDecimal.ZERO,
                            ),
                            JournalEntryLine(
                                accountId = java.util.UUID.fromString("76aee8a0-be1c-3046-ac38-0c0b819d0b1e"),
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
            journalEntryRepository.findByOrganizationIdAndStatusInAndDateBetween(
                orgId,
                listOf(JournalEntryStatus.POSTED, JournalEntryStatus.VOIDED),
                fiscalYear.startDate,
                fiscalYear.endDate,
            ),
        ).thenReturn(entries)

        `when`(accountRepository.findAllById(any<Iterable<java.util.UUID>>()))
            .thenReturn(listOf(cashAccount, expenseAccount, retainedEarnings))
        `when`(accountRepository.findByOrganizationIdAndCode(orgId, "3100"))
            .thenReturn(Optional.of(retainedEarnings))
        `when`(journalEntryRepository.countByOrganizationId(orgId)).thenReturn(1L)
        `when`(journalEntryRepository.save(any<JournalEntry>())).thenAnswer { it.arguments[0] }

        val result = fiscalYearService.closeYear(java.util.UUID.fromString("e70d185f-67cf-3457-bb30-61da0a63102e"), orgId, userId)

        assertThat(result.status).isEqualTo(FiscalYearStatus.CLOSED)

        val entryCaptor = argumentCaptor<JournalEntry>()
        verify(journalEntryRepository).save(entryCaptor.capture())

        val closingEntry = entryCaptor.firstValue
        val reLine = closingEntry.lines.find { it.accountId == java.util.UUID.fromString("92b6a7ae-9ab7-31f4-a3cc-05d7399fecac") }
        assertThat(reLine).isNotNull
        assertThat(reLine!!.debit).isEqualByComparingTo(BigDecimal("500.00"))
        assertThat(reLine.credit).isEqualByComparingTo(BigDecimal.ZERO)
    }

    @Test
    fun `closeYear should skip closing entry when no revenue or expense activity`() {
        val periods =
            listOf(
                createPeriod(1, status = FiscalPeriodStatus.CLOSED),
            )
        val fiscalYear = createFiscalYear(periods = periods)
        `when`(
            fiscalYearRepository.findById(java.util.UUID.fromString("e70d185f-67cf-3457-bb30-61da0a63102e")),
        ).thenReturn(Optional.of(fiscalYear))
        `when`(fiscalYearRepository.save(any<FiscalYear>())).thenAnswer { it.arguments[0] }
        `when`(journalEntryRepository.existsByOrganizationIdAndSourceReference(orgId, "YEAR-END-CLOSE-fy-1"))
            .thenReturn(false)

        `when`(
            journalEntryRepository.findByOrganizationIdAndStatusInAndDateBetween(
                orgId,
                listOf(JournalEntryStatus.POSTED, JournalEntryStatus.VOIDED),
                fiscalYear.startDate,
                fiscalYear.endDate,
            ),
        ).thenReturn(emptyList())

        `when`(accountRepository.findAllById(any<Iterable<java.util.UUID>>()))
            .thenReturn(emptyList())

        val result = fiscalYearService.closeYear(java.util.UUID.fromString("e70d185f-67cf-3457-bb30-61da0a63102e"), orgId, userId)

        assertThat(result.status).isEqualTo(FiscalYearStatus.CLOSED)
        assertThat(result.closingEntryId).isNull()
        verify(journalEntryRepository, never()).save(any<JournalEntry>())
    }

    @Test
    fun `findPeriodForDate should return matching open period`() {
        val fiscalYear = createFiscalYear()
        `when`(fiscalYearRepository.findByOrganizationId(orgId))
            .thenReturn(listOf(fiscalYear))

        val result =
            fiscalYearService.findPeriodForDate(
                orgId,
                LocalDate.of(2026, 1, 15),
            )

        assertThat(result).isInstanceOf(FiscalYearService.PeriodLookupResult.Found::class.java)
        val found = result as FiscalYearService.PeriodLookupResult.Found
        assertThat(found.period.status).isEqualTo(FiscalPeriodStatus.OPEN)
    }

    @Test
    fun `findPeriodForDate should return NoFiscalYears when no fiscal year exists`() {
        `when`(fiscalYearRepository.findByOrganizationId(orgId))
            .thenReturn(emptyList())

        val result =
            fiscalYearService.findPeriodForDate(
                orgId,
                LocalDate.of(2026, 1, 15),
            )

        assertThat(result).isEqualTo(FiscalYearService.PeriodLookupResult.NoFiscalYears)
    }

    @Test
    fun `findPeriodForDate should return NotFound when date is outside fiscal year`() {
        val fiscalYear = createFiscalYear()
        `when`(fiscalYearRepository.findByOrganizationId(orgId))
            .thenReturn(listOf(fiscalYear))

        val result =
            fiscalYearService.findPeriodForDate(
                orgId,
                LocalDate.of(2025, 6, 15),
            )

        assertThat(result).isEqualTo(FiscalYearService.PeriodLookupResult.NotFound)
    }

    @Test
    fun `findPeriodForDate should return reopened period`() {
        val periods =
            listOf(
                createPeriod(1, status = FiscalPeriodStatus.REOPENED),
            )
        val fiscalYear = createFiscalYear(periods = periods)
        `when`(fiscalYearRepository.findByOrganizationId(orgId))
            .thenReturn(listOf(fiscalYear))

        val result =
            fiscalYearService.findPeriodForDate(
                orgId,
                LocalDate.of(2026, 1, 15),
            )

        assertThat(result).isInstanceOf(FiscalYearService.PeriodLookupResult.Found::class.java)
        val found = result as FiscalYearService.PeriodLookupResult.Found
        assertThat(found.period.status).isEqualTo(FiscalPeriodStatus.REOPENED)
    }

    private fun createFiscalYear(
        id: java.util.UUID = java.util.UUID.ofEpochMillis(System.currentTimeMillis()),
        name: String = "FY 2026",
        startDate: LocalDate = LocalDate.of(2026, 1, 1),
        endDate: LocalDate = LocalDate.of(2026, 12, 31),
        status: FiscalYearStatus = FiscalYearStatus.ACTIVE,
        periods: List<FiscalPeriod>? = null,
    ): FiscalYear =
        FiscalYear(
            id = id,
            name = name,
            startDate = startDate,
            endDate = endDate,
            status = status,
            organizationId = orgId,
            periods =
                periods ?: listOf(
                    createPeriod(1),
                ),
        )

    private fun createPeriod(
        number: Int,
        status: FiscalPeriodStatus = FiscalPeriodStatus.OPEN,
    ): FiscalPeriod =
        FiscalPeriod(
            periodNumber = number,
            name = "January 2026",
            startDate = LocalDate.of(2026, 1, 1),
            endDate = LocalDate.of(2026, 1, 31),
            status = status,
        )
}
