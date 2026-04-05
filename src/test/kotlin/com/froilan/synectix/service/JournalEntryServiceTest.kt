package com.froilan.synectix.service

import com.froilan.synectix.dto.CreateJournalEntryRequest
import com.froilan.synectix.dto.JournalEntryLineRequest
import com.froilan.synectix.model.Account
import com.froilan.synectix.model.AccountType
import com.froilan.synectix.model.JournalEntry
import com.froilan.synectix.model.JournalEntryLine
import com.froilan.synectix.model.JournalEntrySource
import com.froilan.synectix.model.JournalEntryStatus
import com.froilan.synectix.repository.AccountRepository
import com.froilan.synectix.repository.JournalEntryRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

class JournalEntryServiceTest {
    private lateinit var journalEntryService: JournalEntryService
    private lateinit var journalEntryRepository: JournalEntryRepository
    private lateinit var accountRepository: AccountRepository

    private val orgId = "org-123"
    private val createdBy = "user-1"

    @BeforeEach
    fun setup() {
        journalEntryRepository = mock(JournalEntryRepository::class.java)
        accountRepository = mock(AccountRepository::class.java)
        journalEntryService =
            JournalEntryService(
                journalEntryRepository = journalEntryRepository,
                accountRepository = accountRepository,
            )
    }

    @Test
    fun `create should save balanced journal entry as DRAFT`() {
        val cashAccount = createMockAccount(id = "acc-1", code = "1000", name = "Cash", type = AccountType.ASSET, orgId = orgId)
        val revenueAccount = createMockAccount(id = "acc-2", code = "4000", name = "Revenue", type = AccountType.REVENUE, orgId = orgId)

        `when`(accountRepository.findAllById(listOf("acc-1", "acc-2"))).thenReturn(listOf(cashAccount, revenueAccount))
        `when`(journalEntryRepository.countByOrganizationId(orgId)).thenReturn(0L)
        `when`(journalEntryRepository.save(any<JournalEntry>())).thenAnswer { it.arguments[0] }

        val request =
            CreateJournalEntryRequest(
                date = LocalDate.of(2026, 1, 15),
                description = "Sale received",
                lines =
                    listOf(
                        JournalEntryLineRequest(accountId = "acc-1", debit = BigDecimal("100.00"), credit = BigDecimal.ZERO),
                        JournalEntryLineRequest(accountId = "acc-2", debit = BigDecimal.ZERO, credit = BigDecimal("100.00")),
                    ),
            )

        val result = journalEntryService.createJournalEntry(request, orgId, createdBy)

        assertThat(result.status).isEqualTo(JournalEntryStatus.DRAFT)
        assertThat(result.source).isEqualTo(JournalEntrySource.MANUAL)
        assertThat(result.description).isEqualTo("Sale received")
        assertThat(result.organizationId).isEqualTo(orgId)
        assertThat(result.createdBy).isEqualTo(createdBy)
        assertThat(result.lines).hasSize(2)
        assertThat(result.lines[0].debit).isEqualByComparingTo(BigDecimal("100.00"))
        assertThat(result.lines[1].credit).isEqualByComparingTo(BigDecimal("100.00"))

        val captor = argumentCaptor<JournalEntry>()
        verify(journalEntryRepository).save(captor.capture())
        assertThat(captor.firstValue.status).isEqualTo(JournalEntryStatus.DRAFT)
    }

    @Test
    fun `create should throw when less than 2 lines`() {
        val request =
            CreateJournalEntryRequest(
                date = LocalDate.of(2026, 1, 15),
                description = "Bad entry",
                lines =
                    listOf(
                        JournalEntryLineRequest(accountId = "acc-1", debit = BigDecimal("100.00"), credit = BigDecimal.ZERO),
                    ),
            )

        val exception =
            assertThrows<IllegalArgumentException> {
                journalEntryService.createJournalEntry(request, orgId, createdBy)
            }
        assertThat(exception.message).contains("at least 2 line items")
    }

    @Test
    fun `create should throw when line has both debit and credit`() {
        val request =
            CreateJournalEntryRequest(
                date = LocalDate.of(2026, 1, 15),
                description = "Bad entry",
                lines =
                    listOf(
                        JournalEntryLineRequest(accountId = "acc-1", debit = BigDecimal("100.00"), credit = BigDecimal("50.00")),
                        JournalEntryLineRequest(accountId = "acc-2", debit = BigDecimal.ZERO, credit = BigDecimal("50.00")),
                    ),
            )

        val exception =
            assertThrows<IllegalArgumentException> {
                journalEntryService.createJournalEntry(request, orgId, createdBy)
            }
        assertThat(exception.message).contains("cannot have both debit and credit")
    }

    @Test
    fun `create should throw when line has zero debit and credit`() {
        val request =
            CreateJournalEntryRequest(
                date = LocalDate.of(2026, 1, 15),
                description = "Bad entry",
                lines =
                    listOf(
                        JournalEntryLineRequest(accountId = "acc-1", debit = BigDecimal.ZERO, credit = BigDecimal.ZERO),
                        JournalEntryLineRequest(accountId = "acc-2", debit = BigDecimal("100.00"), credit = BigDecimal.ZERO),
                    ),
            )

        val exception =
            assertThrows<IllegalArgumentException> {
                journalEntryService.createJournalEntry(request, orgId, createdBy)
            }
        assertThat(exception.message).contains("must have either a debit or credit amount")
    }

    @Test
    fun `create should throw when entry is unbalanced`() {
        val request =
            CreateJournalEntryRequest(
                date = LocalDate.of(2026, 1, 15),
                description = "Unbalanced entry",
                lines =
                    listOf(
                        JournalEntryLineRequest(accountId = "acc-1", debit = BigDecimal("100.00"), credit = BigDecimal.ZERO),
                        JournalEntryLineRequest(accountId = "acc-2", debit = BigDecimal.ZERO, credit = BigDecimal("50.00")),
                    ),
            )

        val exception =
            assertThrows<IllegalArgumentException> {
                journalEntryService.createJournalEntry(request, orgId, createdBy)
            }
        assertThat(exception.message).contains("must balance")
    }

    @Test
    fun `create should throw when account not found`() {
        `when`(accountRepository.findAllById(listOf("acc-1", "acc-missing"))).thenReturn(
            listOf(createMockAccount(id = "acc-1", code = "1000", name = "Cash", type = AccountType.ASSET, orgId = orgId)),
        )

        val request =
            CreateJournalEntryRequest(
                date = LocalDate.of(2026, 1, 15),
                description = "Missing account",
                lines =
                    listOf(
                        JournalEntryLineRequest(accountId = "acc-1", debit = BigDecimal("100.00"), credit = BigDecimal.ZERO),
                        JournalEntryLineRequest(accountId = "acc-missing", debit = BigDecimal.ZERO, credit = BigDecimal("100.00")),
                    ),
            )

        val exception =
            assertThrows<IllegalArgumentException> {
                journalEntryService.createJournalEntry(request, orgId, createdBy)
            }
        assertThat(exception.message).contains("not found")
    }

    @Test
    fun `create should throw when account is inactive`() {
        val inactiveAccount =
            createMockAccount(
                id = "acc-2",
                code = "4000",
                name = "Old Revenue",
                type = AccountType.REVENUE,
                orgId = orgId,
            ).copy(isActive = false)
        val cashAccount = createMockAccount(id = "acc-1", code = "1000", name = "Cash", type = AccountType.ASSET, orgId = orgId)

        `when`(accountRepository.findAllById(listOf("acc-1", "acc-2"))).thenReturn(listOf(cashAccount, inactiveAccount))

        val request =
            CreateJournalEntryRequest(
                date = LocalDate.of(2026, 1, 15),
                description = "Inactive account entry",
                lines =
                    listOf(
                        JournalEntryLineRequest(accountId = "acc-1", debit = BigDecimal("100.00"), credit = BigDecimal.ZERO),
                        JournalEntryLineRequest(accountId = "acc-2", debit = BigDecimal.ZERO, credit = BigDecimal("100.00")),
                    ),
            )

        val exception =
            assertThrows<IllegalArgumentException> {
                journalEntryService.createJournalEntry(request, orgId, createdBy)
            }
        assertThat(exception.message).contains("inactive")
    }

    @Test
    fun `create should throw when account is in different org`() {
        val cashAccount = createMockAccount(id = "acc-1", code = "1000", name = "Cash", type = AccountType.ASSET, orgId = orgId)
        val otherOrgAccount =
            createMockAccount(id = "acc-2", code = "4000", name = "Revenue", type = AccountType.REVENUE, orgId = "other-org")

        `when`(accountRepository.findAllById(listOf("acc-1", "acc-2"))).thenReturn(listOf(cashAccount, otherOrgAccount))

        val request =
            CreateJournalEntryRequest(
                date = LocalDate.of(2026, 1, 15),
                description = "Wrong org entry",
                lines =
                    listOf(
                        JournalEntryLineRequest(accountId = "acc-1", debit = BigDecimal("100.00"), credit = BigDecimal.ZERO),
                        JournalEntryLineRequest(accountId = "acc-2", debit = BigDecimal.ZERO, credit = BigDecimal("100.00")),
                    ),
            )

        val exception =
            assertThrows<IllegalArgumentException> {
                journalEntryService.createJournalEntry(request, orgId, createdBy)
            }
        assertThat(exception.message).contains("not found")
    }

    @Test
    fun `create should generate sequential entry number`() {
        val cashAccount = createMockAccount(id = "acc-1", code = "1000", name = "Cash", type = AccountType.ASSET, orgId = orgId)
        val revenueAccount = createMockAccount(id = "acc-2", code = "4000", name = "Revenue", type = AccountType.REVENUE, orgId = orgId)

        `when`(accountRepository.findAllById(listOf("acc-1", "acc-2"))).thenReturn(listOf(cashAccount, revenueAccount))
        `when`(journalEntryRepository.countByOrganizationId(orgId)).thenReturn(5L)
        `when`(journalEntryRepository.save(any<JournalEntry>())).thenAnswer { it.arguments[0] }

        val request =
            CreateJournalEntryRequest(
                date = LocalDate.of(2026, 1, 15),
                description = "Sequential test",
                lines =
                    listOf(
                        JournalEntryLineRequest(accountId = "acc-1", debit = BigDecimal("100.00"), credit = BigDecimal.ZERO),
                        JournalEntryLineRequest(accountId = "acc-2", debit = BigDecimal.ZERO, credit = BigDecimal("100.00")),
                    ),
            )

        val result = journalEntryService.createJournalEntry(request, orgId, createdBy)

        assertThat(result.entryNumber).isEqualTo("JE-0006")
    }

    @Test
    fun `post should change status from DRAFT to POSTED`() {
        val draftEntry =
            createMockEntry(
                id = "entry-1",
                entryNumber = "JE-0001",
                status = JournalEntryStatus.DRAFT,
                lines =
                    listOf(
                        JournalEntryLine(
                            accountId = "acc-1",
                            accountCode = "1000",
                            accountName = "Cash",
                            debit = BigDecimal("100.00"),
                            credit = BigDecimal.ZERO,
                        ),
                        JournalEntryLine(
                            accountId = "acc-2",
                            accountCode = "4000",
                            accountName = "Revenue",
                            debit = BigDecimal.ZERO,
                            credit = BigDecimal("100.00"),
                        ),
                    ),
                orgId = orgId,
            )

        `when`(journalEntryRepository.findById("entry-1")).thenReturn(Optional.of(draftEntry))
        `when`(journalEntryRepository.save(any<JournalEntry>())).thenAnswer { it.arguments[0] }

        val result = journalEntryService.postJournalEntry("entry-1", orgId)

        assertThat(result.status).isEqualTo(JournalEntryStatus.POSTED)
        assertThat(result.postedAt).isNotNull()

        val captor = argumentCaptor<JournalEntry>()
        verify(journalEntryRepository).save(captor.capture())
        assertThat(captor.firstValue.status).isEqualTo(JournalEntryStatus.POSTED)
    }

    @Test
    fun `post should throw when entry is not DRAFT`() {
        val postedEntry =
            createMockEntry(
                id = "entry-1",
                entryNumber = "JE-0001",
                status = JournalEntryStatus.POSTED,
                lines =
                    listOf(
                        JournalEntryLine(
                            accountId = "acc-1",
                            accountCode = "1000",
                            accountName = "Cash",
                            debit = BigDecimal("100.00"),
                            credit = BigDecimal.ZERO,
                        ),
                        JournalEntryLine(
                            accountId = "acc-2",
                            accountCode = "4000",
                            accountName = "Revenue",
                            debit = BigDecimal.ZERO,
                            credit = BigDecimal("100.00"),
                        ),
                    ),
                orgId = orgId,
            )

        `when`(journalEntryRepository.findById("entry-1")).thenReturn(Optional.of(postedEntry))

        val exception =
            assertThrows<IllegalArgumentException> {
                journalEntryService.postJournalEntry("entry-1", orgId)
            }
        assertThat(exception.message).contains("Only draft entries can be posted")
    }

    @Test
    fun `void should change status to VOIDED and create reversing entry`() {
        val postedEntry =
            createMockEntry(
                id = "entry-1",
                entryNumber = "JE-0001",
                status = JournalEntryStatus.POSTED,
                lines =
                    listOf(
                        JournalEntryLine(
                            accountId = "acc-1",
                            accountCode = "1000",
                            accountName = "Cash",
                            debit = BigDecimal("200.00"),
                            credit = BigDecimal.ZERO,
                        ),
                        JournalEntryLine(
                            accountId = "acc-2",
                            accountCode = "4000",
                            accountName = "Revenue",
                            debit = BigDecimal.ZERO,
                            credit = BigDecimal("200.00"),
                        ),
                    ),
                orgId = orgId,
            )

        `when`(journalEntryRepository.findById("entry-1")).thenReturn(Optional.of(postedEntry))
        `when`(journalEntryRepository.countByOrganizationId(orgId)).thenReturn(1L)
        `when`(journalEntryRepository.save(any<JournalEntry>())).thenAnswer { it.arguments[0] }

        val result = journalEntryService.voidJournalEntry("entry-1", orgId, "Incorrect amount")

        assertThat(result.status).isEqualTo(JournalEntryStatus.VOIDED)
        assertThat(result.voidedAt).isNotNull()
        assertThat(result.voidReason).isEqualTo("Incorrect amount")

        val captor = argumentCaptor<JournalEntry>()
        verify(journalEntryRepository, times(2)).save(captor.capture())

        val allSaved = captor.allValues
        assertThat(allSaved).hasSize(2)

        // First save is the voided entry
        val voidedSave = allSaved[0]
        assertThat(voidedSave.status).isEqualTo(JournalEntryStatus.VOIDED)

        // Second save is the reversing entry
        val reversingSave = allSaved[1]
        assertThat(reversingSave.status).isEqualTo(JournalEntryStatus.POSTED)
        assertThat(reversingSave.source).isEqualTo(JournalEntrySource.SYSTEM)
        assertThat(reversingSave.description).contains("Reversal of JE-0001")
        assertThat(reversingSave.lines[0].debit).isEqualByComparingTo(BigDecimal.ZERO)
        assertThat(reversingSave.lines[0].credit).isEqualByComparingTo(BigDecimal("200.00"))
        assertThat(reversingSave.lines[1].debit).isEqualByComparingTo(BigDecimal("200.00"))
        assertThat(reversingSave.lines[1].credit).isEqualByComparingTo(BigDecimal.ZERO)
    }

    @Test
    fun `void should throw when entry is not POSTED`() {
        val draftEntry =
            createMockEntry(
                id = "entry-1",
                entryNumber = "JE-0001",
                status = JournalEntryStatus.DRAFT,
                lines =
                    listOf(
                        JournalEntryLine(
                            accountId = "acc-1",
                            accountCode = "1000",
                            accountName = "Cash",
                            debit = BigDecimal("100.00"),
                            credit = BigDecimal.ZERO,
                        ),
                        JournalEntryLine(
                            accountId = "acc-2",
                            accountCode = "4000",
                            accountName = "Revenue",
                            debit = BigDecimal.ZERO,
                            credit = BigDecimal("100.00"),
                        ),
                    ),
                orgId = orgId,
            )

        `when`(journalEntryRepository.findById("entry-1")).thenReturn(Optional.of(draftEntry))

        val exception =
            assertThrows<IllegalArgumentException> {
                journalEntryService.voidJournalEntry("entry-1", orgId, "Some reason")
            }
        assertThat(exception.message).contains("Only posted entries can be voided")
    }

    @Test
    fun `getAccountBalance should sum debits and credits from posted entries`() {
        val account = createMockAccount(id = "acc-1", code = "1000", name = "Cash", type = AccountType.ASSET, orgId = orgId)
        `when`(accountRepository.findById("acc-1")).thenReturn(Optional.of(account))

        val entries =
            listOf(
                createMockEntry(
                    id = "entry-1",
                    entryNumber = "JE-0001",
                    status = JournalEntryStatus.POSTED,
                    lines =
                        listOf(
                            JournalEntryLine(
                                accountId = "acc-1",
                                accountCode = "1000",
                                accountName = "Cash",
                                debit = BigDecimal("500.00"),
                                credit = BigDecimal.ZERO,
                            ),
                            JournalEntryLine(
                                accountId = "acc-2",
                                accountCode = "4000",
                                accountName = "Revenue",
                                debit = BigDecimal.ZERO,
                                credit = BigDecimal("500.00"),
                            ),
                        ),
                    orgId = orgId,
                ),
                createMockEntry(
                    id = "entry-2",
                    entryNumber = "JE-0002",
                    status = JournalEntryStatus.POSTED,
                    lines =
                        listOf(
                            JournalEntryLine(
                                accountId = "acc-3",
                                accountCode = "5000",
                                accountName = "Expense",
                                debit = BigDecimal("150.00"),
                                credit = BigDecimal.ZERO,
                            ),
                            JournalEntryLine(
                                accountId = "acc-1",
                                accountCode = "1000",
                                accountName = "Cash",
                                debit = BigDecimal.ZERO,
                                credit = BigDecimal("150.00"),
                            ),
                        ),
                    orgId = orgId,
                ),
            )

        `when`(journalEntryRepository.findByOrganizationIdAndStatus(orgId, JournalEntryStatus.POSTED)).thenReturn(entries)

        val result = journalEntryService.getAccountBalance("acc-1", orgId)

        assertThat(result.accountId).isEqualTo("acc-1")
        assertThat(result.accountCode).isEqualTo("1000")
        assertThat(result.accountName).isEqualTo("Cash")
        assertThat(result.accountType).isEqualTo("ASSET")
        assertThat(result.totalDebits).isEqualByComparingTo(BigDecimal("500.00"))
        assertThat(result.totalCredits).isEqualByComparingTo(BigDecimal("150.00"))
        // ASSET: balance = debits - credits
        assertThat(result.balance).isEqualByComparingTo(BigDecimal("350.00"))
    }

    @Test
    fun `getTrialBalance should return all account balances`() {
        val cashAccount = createMockAccount(id = "acc-1", code = "1000", name = "Cash", type = AccountType.ASSET, orgId = orgId)
        val revenueAccount = createMockAccount(id = "acc-2", code = "4000", name = "Revenue", type = AccountType.REVENUE, orgId = orgId)

        val entries =
            listOf(
                createMockEntry(
                    id = "entry-1",
                    entryNumber = "JE-0001",
                    status = JournalEntryStatus.POSTED,
                    lines =
                        listOf(
                            JournalEntryLine(
                                accountId = "acc-1",
                                accountCode = "1000",
                                accountName = "Cash",
                                debit = BigDecimal("300.00"),
                                credit = BigDecimal.ZERO,
                            ),
                            JournalEntryLine(
                                accountId = "acc-2",
                                accountCode = "4000",
                                accountName = "Revenue",
                                debit = BigDecimal.ZERO,
                                credit = BigDecimal("300.00"),
                            ),
                        ),
                    orgId = orgId,
                ),
            )

        `when`(journalEntryRepository.findByOrganizationIdAndStatus(orgId, JournalEntryStatus.POSTED)).thenReturn(entries)
        `when`(accountRepository.findAllById(listOf("acc-1", "acc-2"))).thenReturn(listOf(cashAccount, revenueAccount))

        val result = journalEntryService.getTrialBalance(orgId)

        assertThat(result.accounts).hasSize(2)
        assertThat(result.totalDebits).isEqualByComparingTo(BigDecimal("300.00"))
        assertThat(result.totalCredits).isEqualByComparingTo(BigDecimal("300.00"))
        // Trial balance must balance: total debits == total credits
        assertThat(result.totalDebits).isEqualByComparingTo(result.totalCredits)

        val cashBalance = result.accounts.find { it.accountId == "acc-1" }
        assertThat(cashBalance).isNotNull
        assertThat(cashBalance!!.totalDebits).isEqualByComparingTo(BigDecimal("300.00"))
        assertThat(cashBalance.balance).isEqualByComparingTo(BigDecimal("300.00"))

        val revenueBalance = result.accounts.find { it.accountId == "acc-2" }
        assertThat(revenueBalance).isNotNull
        assertThat(revenueBalance!!.totalCredits).isEqualByComparingTo(BigDecimal("300.00"))
        // REVENUE: balance = credits - debits
        assertThat(revenueBalance.balance).isEqualByComparingTo(BigDecimal("300.00"))
    }

    private fun createMockAccount(
        id: String = "acc-1",
        code: String = "1000",
        name: String = "Cash",
        type: AccountType = AccountType.ASSET,
        orgId: String = "org-123",
    ) = Account(
        id = id,
        code = code,
        name = name,
        type = type,
        organizationId = orgId,
        isActive = true,
        isSystemAccount = false,
    )

    private fun createMockEntry(
        id: String = "entry-1",
        entryNumber: String = "JE-0001",
        status: JournalEntryStatus = JournalEntryStatus.DRAFT,
        lines: List<JournalEntryLine> = emptyList(),
        orgId: String = "org-123",
    ) = JournalEntry(
        id = id,
        entryNumber = entryNumber,
        date = LocalDate.of(2026, 1, 15),
        description = "Test entry",
        organizationId = orgId,
        status = status,
        lines = lines,
        createdBy = "user-1",
    )
}
