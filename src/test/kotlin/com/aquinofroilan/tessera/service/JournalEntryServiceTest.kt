package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateJournalEntryRequest
import com.aquinofroilan.tessera.dto.JournalEntryLineRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.model.Account
import com.aquinofroilan.tessera.model.AccountType
import com.aquinofroilan.tessera.model.JournalEntry
import com.aquinofroilan.tessera.model.JournalEntryLine
import com.aquinofroilan.tessera.model.JournalEntrySource
import com.aquinofroilan.tessera.model.JournalEntryStatus
import com.aquinofroilan.tessera.repository.AccountRepository
import com.aquinofroilan.tessera.repository.AccountTotals
import com.aquinofroilan.tessera.repository.JournalEntryRepository
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
import org.mockito.kotlin.doThrow
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

class JournalEntryServiceTest {
    private lateinit var journalEntryService: JournalEntryService
    private lateinit var journalEntryRepository: JournalEntryRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var fiscalYearService: FiscalYearService
    private lateinit var entryNumberGenerator: JournalEntryNumberGenerator

    private val orgId = java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d")
    private val createdBy = java.util.UUID.fromString("1db2395f-13ba-3d37-9d2b-f77d3eb3aa2e")

    @BeforeEach
    fun setup() {
        journalEntryRepository = mock(JournalEntryRepository::class.java)
        accountRepository = mock(AccountRepository::class.java)
        fiscalYearService = mock(FiscalYearService::class.java)
        entryNumberGenerator = JournalEntryNumberGenerator(journalEntryRepository)
        journalEntryService =
            JournalEntryService(
                journalEntryRepository = journalEntryRepository,
                accountRepository = accountRepository,
                fiscalYearService = fiscalYearService,
                entryNumberGenerator = entryNumberGenerator,
            )
    }

    @Test
    fun `create should save balanced journal entry as DRAFT`() {
        val cashAccount =
            createMockAccount(
                id = java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"),
                code = "1000",
                name = "Cash",
                type = AccountType.ASSET,
                orgId = orgId,
            )
        val revenueAccount =
            createMockAccount(
                id = java.util.UUID.fromString("19770c7a-8b4f-3b6e-adb1-3631faff91ec"),
                code = "4000",
                name = "Revenue",
                type = AccountType.REVENUE,
                orgId = orgId,
            )

        `when`(
            accountRepository.findAllById(
                listOf(
                    java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"),
                    java.util.UUID.fromString("19770c7a-8b4f-3b6e-adb1-3631faff91ec"),
                ),
            ),
        ).thenReturn(listOf(cashAccount, revenueAccount))
        `when`(journalEntryRepository.countByOrganizationId(orgId)).thenReturn(0L)
        `when`(journalEntryRepository.save(any<JournalEntry>())).thenAnswer { it.arguments[0] }

        val request =
            CreateJournalEntryRequest(
                date = LocalDate.of(2026, 1, 15),
                description = "Sale received",
                lines =
                    listOf(
                        JournalEntryLineRequest(
                            accountId = java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"),
                            debit = BigDecimal("100.00"),
                            credit = BigDecimal.ZERO,
                        ),
                        JournalEntryLineRequest(
                            accountId = java.util.UUID.fromString("19770c7a-8b4f-3b6e-adb1-3631faff91ec"),
                            debit = BigDecimal.ZERO,
                            credit = BigDecimal("100.00"),
                        ),
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
                        JournalEntryLineRequest(
                            accountId = java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"),
                            debit = BigDecimal("100.00"),
                            credit = BigDecimal.ZERO,
                        ),
                    ),
            )

        val exception =
            assertThrows<BusinessRuleException> {
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
                        JournalEntryLineRequest(
                            accountId = java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"),
                            debit = BigDecimal("100.00"),
                            credit = BigDecimal("50.00"),
                        ),
                        JournalEntryLineRequest(
                            accountId = java.util.UUID.fromString("19770c7a-8b4f-3b6e-adb1-3631faff91ec"),
                            debit = BigDecimal.ZERO,
                            credit = BigDecimal("50.00"),
                        ),
                    ),
            )

        val exception =
            assertThrows<BusinessRuleException> {
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
                        JournalEntryLineRequest(
                            accountId = java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"),
                            debit = BigDecimal.ZERO,
                            credit = BigDecimal.ZERO,
                        ),
                        JournalEntryLineRequest(
                            accountId = java.util.UUID.fromString("19770c7a-8b4f-3b6e-adb1-3631faff91ec"),
                            debit = BigDecimal("100.00"),
                            credit = BigDecimal.ZERO,
                        ),
                    ),
            )

        val exception =
            assertThrows<BusinessRuleException> {
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
                        JournalEntryLineRequest(
                            accountId = java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"),
                            debit = BigDecimal("100.00"),
                            credit = BigDecimal.ZERO,
                        ),
                        JournalEntryLineRequest(
                            accountId = java.util.UUID.fromString("19770c7a-8b4f-3b6e-adb1-3631faff91ec"),
                            debit = BigDecimal.ZERO,
                            credit = BigDecimal("50.00"),
                        ),
                    ),
            )

        val exception =
            assertThrows<BusinessRuleException> {
                journalEntryService.createJournalEntry(request, orgId, createdBy)
            }
        assertThat(exception.message).contains("must balance")
    }

    @Test
    fun `create should throw when account not found`() {
        `when`(
            accountRepository.findAllById(
                listOf(
                    java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"),
                    java.util.UUID.fromString("f9f4cccd-5a62-3777-8b70-12a8106bfcd4"),
                ),
            ),
        ).thenReturn(
            listOf(
                createMockAccount(
                    id = java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"),
                    code = "1000",
                    name = "Cash",
                    type = AccountType.ASSET,
                    orgId = orgId,
                ),
            ),
        )

        val request =
            CreateJournalEntryRequest(
                date = LocalDate.of(2026, 1, 15),
                description = "Missing account",
                lines =
                    listOf(
                        JournalEntryLineRequest(
                            accountId = java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"),
                            debit = BigDecimal("100.00"),
                            credit = BigDecimal.ZERO,
                        ),
                        JournalEntryLineRequest(
                            accountId = java.util.UUID.fromString("f9f4cccd-5a62-3777-8b70-12a8106bfcd4"),
                            debit = BigDecimal.ZERO,
                            credit = BigDecimal("100.00"),
                        ),
                    ),
            )

        val exception =
            assertThrows<BusinessRuleException> {
                journalEntryService.createJournalEntry(request, orgId, createdBy)
            }
        assertThat(exception.message).contains("not found")
    }

    @Test
    fun `create should throw when account is inactive`() {
        val inactiveAccount =
            createMockAccount(
                id = java.util.UUID.fromString("19770c7a-8b4f-3b6e-adb1-3631faff91ec"),
                code = "4000",
                name = "Old Revenue",
                type = AccountType.REVENUE,
                orgId = orgId,
            ).apply { isActive = false }
        val cashAccount =
            createMockAccount(
                id = java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"),
                code = "1000",
                name = "Cash",
                type = AccountType.ASSET,
                orgId = orgId,
            )

        `when`(
            accountRepository.findAllById(
                listOf(
                    java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"),
                    java.util.UUID.fromString("19770c7a-8b4f-3b6e-adb1-3631faff91ec"),
                ),
            ),
        ).thenReturn(listOf(cashAccount, inactiveAccount))

        val request =
            CreateJournalEntryRequest(
                date = LocalDate.of(2026, 1, 15),
                description = "Inactive account entry",
                lines =
                    listOf(
                        JournalEntryLineRequest(
                            accountId = java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"),
                            debit = BigDecimal("100.00"),
                            credit = BigDecimal.ZERO,
                        ),
                        JournalEntryLineRequest(
                            accountId = java.util.UUID.fromString("19770c7a-8b4f-3b6e-adb1-3631faff91ec"),
                            debit = BigDecimal.ZERO,
                            credit = BigDecimal("100.00"),
                        ),
                    ),
            )

        val exception =
            assertThrows<BusinessRuleException> {
                journalEntryService.createJournalEntry(request, orgId, createdBy)
            }
        assertThat(exception.message).contains("inactive")
    }

    @Test
    fun `create should throw when account is in different org`() {
        val cashAccount =
            createMockAccount(
                id = java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"),
                code = "1000",
                name = "Cash",
                type = AccountType.ASSET,
                orgId = orgId,
            )
        val otherOrgAccount =
            createMockAccount(
                id = java.util.UUID.fromString("19770c7a-8b4f-3b6e-adb1-3631faff91ec"),
                code = "1000",
                name = "Cash",
                type = AccountType.REVENUE,
                orgId = java.util.UUID.fromString("fbede99a-0bef-3bf9-ba0b-8d28f050479d"),
            )

        `when`(
            accountRepository.findAllById(
                listOf(
                    java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"),
                    java.util.UUID.fromString("19770c7a-8b4f-3b6e-adb1-3631faff91ec"),
                ),
            ),
        ).thenReturn(listOf(cashAccount, otherOrgAccount))

        val request =
            CreateJournalEntryRequest(
                date = LocalDate.of(2026, 1, 15),
                description = "Wrong org entry",
                lines =
                    listOf(
                        JournalEntryLineRequest(
                            accountId = java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"),
                            debit = BigDecimal("100.00"),
                            credit = BigDecimal.ZERO,
                        ),
                        JournalEntryLineRequest(
                            accountId = java.util.UUID.fromString("19770c7a-8b4f-3b6e-adb1-3631faff91ec"),
                            debit = BigDecimal.ZERO,
                            credit = BigDecimal("100.00"),
                        ),
                    ),
            )

        val exception =
            assertThrows<BusinessRuleException> {
                journalEntryService.createJournalEntry(request, orgId, createdBy)
            }
        assertThat(exception.message).contains("not found")
    }

    @Test
    fun `create should generate sequential entry number`() {
        val cashAccount =
            createMockAccount(
                id = java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"),
                code = "1000",
                name = "Cash",
                type = AccountType.ASSET,
                orgId = orgId,
            )
        val revenueAccount =
            createMockAccount(
                id = java.util.UUID.fromString("19770c7a-8b4f-3b6e-adb1-3631faff91ec"),
                code = "4000",
                name = "Revenue",
                type = AccountType.REVENUE,
                orgId = orgId,
            )

        `when`(
            accountRepository.findAllById(
                listOf(
                    java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"),
                    java.util.UUID.fromString("19770c7a-8b4f-3b6e-adb1-3631faff91ec"),
                ),
            ),
        ).thenReturn(listOf(cashAccount, revenueAccount))
        `when`(journalEntryRepository.countByOrganizationId(orgId)).thenReturn(5L)
        `when`(journalEntryRepository.save(any<JournalEntry>())).thenAnswer { it.arguments[0] }

        val request =
            CreateJournalEntryRequest(
                date = LocalDate.of(2026, 1, 15),
                description = "Sequential test",
                lines =
                    listOf(
                        JournalEntryLineRequest(
                            accountId = java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"),
                            debit = BigDecimal("100.00"),
                            credit = BigDecimal.ZERO,
                        ),
                        JournalEntryLineRequest(
                            accountId = java.util.UUID.fromString("19770c7a-8b4f-3b6e-adb1-3631faff91ec"),
                            debit = BigDecimal.ZERO,
                            credit = BigDecimal("100.00"),
                        ),
                    ),
            )

        val result = journalEntryService.createJournalEntry(request, orgId, createdBy)

        assertThat(result.entryNumber).isEqualTo("JE-0006")
    }

    @Test
    fun `post should change status from DRAFT to POSTED`() {
        val draftEntry =
            createMockEntry(
                id = java.util.UUID.fromString("883cd44c-a464-3674-a0ec-1fb21a7ccd22"),
                entryNumber = "JE-0001",
                status = JournalEntryStatus.DRAFT,
                lines =
                    listOf(
                        JournalEntryLine(
                            accountId = java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"),
                            accountCode = "1000",
                            accountName = "Cash",
                            debit = BigDecimal("100.00"),
                            credit = BigDecimal.ZERO,
                        ),
                        JournalEntryLine(
                            accountId = java.util.UUID.fromString("19770c7a-8b4f-3b6e-adb1-3631faff91ec"),
                            accountCode = "4000",
                            accountName = "Revenue",
                            debit = BigDecimal.ZERO,
                            credit = BigDecimal("100.00"),
                        ),
                    ),
                orgId = orgId,
            )

        `when`(
            journalEntryRepository.findById(java.util.UUID.fromString("883cd44c-a464-3674-a0ec-1fb21a7ccd22")),
        ).thenReturn(Optional.of(draftEntry))
        `when`(journalEntryRepository.save(any<JournalEntry>())).thenAnswer { it.arguments[0] }

        val result = journalEntryService.postJournalEntry(java.util.UUID.fromString("883cd44c-a464-3674-a0ec-1fb21a7ccd22"), orgId)

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
                id = java.util.UUID.fromString("883cd44c-a464-3674-a0ec-1fb21a7ccd22"),
                entryNumber = "JE-0001",
                status = JournalEntryStatus.POSTED,
                lines =
                    listOf(
                        JournalEntryLine(
                            accountId = java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"),
                            accountCode = "1000",
                            accountName = "Cash",
                            debit = BigDecimal("100.00"),
                            credit = BigDecimal.ZERO,
                        ),
                        JournalEntryLine(
                            accountId = java.util.UUID.fromString("19770c7a-8b4f-3b6e-adb1-3631faff91ec"),
                            accountCode = "4000",
                            accountName = "Revenue",
                            debit = BigDecimal.ZERO,
                            credit = BigDecimal("100.00"),
                        ),
                    ),
                orgId = orgId,
            )

        `when`(
            journalEntryRepository.findById(java.util.UUID.fromString("883cd44c-a464-3674-a0ec-1fb21a7ccd22")),
        ).thenReturn(Optional.of(postedEntry))

        val exception =
            assertThrows<BusinessRuleException> {
                journalEntryService.postJournalEntry(java.util.UUID.fromString("883cd44c-a464-3674-a0ec-1fb21a7ccd22"), orgId)
            }
        assertThat(exception.message).contains("Only draft entries can be posted")
    }

    @Test
    fun `void should change status to VOIDED and create reversing entry`() {
        val postedEntry =
            createMockEntry(
                id = java.util.UUID.fromString("883cd44c-a464-3674-a0ec-1fb21a7ccd22"),
                entryNumber = "JE-0001",
                status = JournalEntryStatus.POSTED,
                lines =
                    listOf(
                        JournalEntryLine(
                            accountId = java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"),
                            accountCode = "1000",
                            accountName = "Cash",
                            debit = BigDecimal("200.00"),
                            credit = BigDecimal.ZERO,
                        ),
                        JournalEntryLine(
                            accountId = java.util.UUID.fromString("19770c7a-8b4f-3b6e-adb1-3631faff91ec"),
                            accountCode = "4000",
                            accountName = "Revenue",
                            debit = BigDecimal.ZERO,
                            credit = BigDecimal("200.00"),
                        ),
                    ),
                orgId = orgId,
            )

        `when`(
            journalEntryRepository.findById(java.util.UUID.fromString("883cd44c-a464-3674-a0ec-1fb21a7ccd22")),
        ).thenReturn(Optional.of(postedEntry))
        `when`(journalEntryRepository.countByOrganizationId(orgId)).thenReturn(1L)
        `when`(journalEntryRepository.save(any<JournalEntry>())).thenAnswer { it.arguments[0] }

        val result =
            journalEntryService.voidJournalEntry(
                java.util.UUID.fromString("883cd44c-a464-3674-a0ec-1fb21a7ccd22"),
                orgId,
                "Incorrect amount",
            )

        assertThat(result.status).isEqualTo(JournalEntryStatus.VOIDED)
        assertThat(result.voidedAt).isNotNull()
        assertThat(result.voidReason).isEqualTo("Incorrect amount")

        val captor = argumentCaptor<JournalEntry>()
        verify(journalEntryRepository, times(2)).save(captor.capture())

        val allSaved = captor.allValues
        assertThat(allSaved).hasSize(2)

        val voidedSave = allSaved[0]
        assertThat(voidedSave.status).isEqualTo(JournalEntryStatus.VOIDED)

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
                id = java.util.UUID.fromString("883cd44c-a464-3674-a0ec-1fb21a7ccd22"),
                entryNumber = "JE-0001",
                status = JournalEntryStatus.DRAFT,
                lines =
                    listOf(
                        JournalEntryLine(
                            accountId = java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"),
                            accountCode = "1000",
                            accountName = "Cash",
                            debit = BigDecimal("100.00"),
                            credit = BigDecimal.ZERO,
                        ),
                        JournalEntryLine(
                            accountId = java.util.UUID.fromString("19770c7a-8b4f-3b6e-adb1-3631faff91ec"),
                            accountCode = "4000",
                            accountName = "Revenue",
                            debit = BigDecimal.ZERO,
                            credit = BigDecimal("100.00"),
                        ),
                    ),
                orgId = orgId,
            )

        `when`(
            journalEntryRepository.findById(java.util.UUID.fromString("883cd44c-a464-3674-a0ec-1fb21a7ccd22")),
        ).thenReturn(Optional.of(draftEntry))

        val exception =
            assertThrows<BusinessRuleException> {
                journalEntryService.voidJournalEntry(
                    java.util.UUID.fromString("883cd44c-a464-3674-a0ec-1fb21a7ccd22"),
                    orgId,
                    "Some reason",
                )
            }
        assertThat(exception.message).contains("Only posted entries can be voided")
    }

    @Test
    fun `getAccountBalance should sum debits and credits from posted entries`() {
        val account =
            createMockAccount(
                id = java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"),
                code = "1000",
                name = "Cash",
                type = AccountType.ASSET,
                orgId = orgId,
            )
        `when`(
            accountRepository.findById(java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a")),
        ).thenReturn(Optional.of(account))
        `when`(
            journalEntryRepository.aggregateAccountTotals(
                orgId,
                listOf(java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a")),
                null,
                null,
            ),
        ).thenReturn(
            mapOf(
                java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a") to
                    AccountTotals(BigDecimal("500.00"), BigDecimal("150.00")),
            ),
        )

        val result = journalEntryService.getAccountBalance(java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"), orgId)

        assertThat(result.accountId).isEqualTo(java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"))
        assertThat(result.accountCode).isEqualTo("1000")
        assertThat(result.accountName).isEqualTo("Cash")
        assertThat(result.accountType).isEqualTo("ASSET")
        assertThat(result.totalDebits).isEqualByComparingTo(BigDecimal("500.00"))
        assertThat(result.totalCredits).isEqualByComparingTo(BigDecimal("150.00"))
        assertThat(result.balance).isEqualByComparingTo(BigDecimal("350.00"))
    }

    @Test
    fun `getTrialBalance should return all account balances`() {
        val cashAccount =
            createMockAccount(
                id = java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"),
                code = "1000",
                name = "Cash",
                type = AccountType.ASSET,
                orgId = orgId,
            )
        val revenueAccount =
            createMockAccount(
                id = java.util.UUID.fromString("19770c7a-8b4f-3b6e-adb1-3631faff91ec"),
                code = "4000",
                name = "Revenue",
                type = AccountType.REVENUE,
                orgId = orgId,
            )

        `when`(accountRepository.findByOrganizationIdAndIsActive(orgId, true)).thenReturn(listOf(cashAccount, revenueAccount))
        `when`(
            journalEntryRepository.aggregateAccountTotals(
                orgId,
                listOf(
                    java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"),
                    java.util.UUID.fromString("19770c7a-8b4f-3b6e-adb1-3631faff91ec"),
                ),
                null,
                null,
            ),
        ).thenReturn(
            mapOf(
                java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a") to
                    AccountTotals(BigDecimal("300.00"), BigDecimal.ZERO),
                java.util.UUID.fromString("19770c7a-8b4f-3b6e-adb1-3631faff91ec") to
                    AccountTotals(BigDecimal.ZERO, BigDecimal("300.00")),
            ),
        )

        val result = journalEntryService.getTrialBalance(orgId)

        assertThat(result.accounts).hasSize(2)
        assertThat(result.totalDebits).isEqualByComparingTo(BigDecimal("300.00"))
        assertThat(result.totalCredits).isEqualByComparingTo(BigDecimal("300.00"))
        assertThat(result.totalDebits).isEqualByComparingTo(result.totalCredits)

        val cashBalance = result.accounts.find { it.accountId == java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a") }
        assertThat(cashBalance).isNotNull
        assertThat(cashBalance!!.totalDebits).isEqualByComparingTo(BigDecimal("300.00"))
        assertThat(cashBalance.balance).isEqualByComparingTo(BigDecimal("300.00"))

        val revenueBalance = result.accounts.find { it.accountId == java.util.UUID.fromString("19770c7a-8b4f-3b6e-adb1-3631faff91ec") }
        assertThat(revenueBalance).isNotNull
        assertThat(revenueBalance!!.totalCredits).isEqualByComparingTo(BigDecimal("300.00"))
        assertThat(revenueBalance.balance).isEqualByComparingTo(BigDecimal("300.00"))
    }

    @Test
    fun `create should succeed when no fiscal year exists`() {
        val cashAccount =
            createMockAccount(
                id = java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"),
                code = "1000",
                name = "Cash",
                type = AccountType.ASSET,
                orgId = orgId,
            )
        val revenueAccount =
            createMockAccount(
                id = java.util.UUID.fromString("19770c7a-8b4f-3b6e-adb1-3631faff91ec"),
                code = "4000",
                name = "Revenue",
                type = AccountType.REVENUE,
                orgId = orgId,
            )

        `when`(
            accountRepository.findAllById(
                listOf(
                    java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"),
                    java.util.UUID.fromString("19770c7a-8b4f-3b6e-adb1-3631faff91ec"),
                ),
            ),
        ).thenReturn(listOf(cashAccount, revenueAccount))
        `when`(journalEntryRepository.countByOrganizationId(orgId)).thenReturn(0L)
        `when`(journalEntryRepository.save(any<JournalEntry>())).thenAnswer { it.arguments[0] }

        val request =
            CreateJournalEntryRequest(
                date = LocalDate.of(2026, 1, 15),
                description = "Test entry",
                lines =
                    listOf(
                        JournalEntryLineRequest(
                            accountId = java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"),
                            debit = BigDecimal("100.00"),
                            credit = BigDecimal.ZERO,
                        ),
                        JournalEntryLineRequest(
                            accountId = java.util.UUID.fromString("19770c7a-8b4f-3b6e-adb1-3631faff91ec"),
                            debit = BigDecimal.ZERO,
                            credit = BigDecimal("100.00"),
                        ),
                    ),
            )

        val result = journalEntryService.createJournalEntry(request, orgId, createdBy)
        assertThat(result.status).isEqualTo(JournalEntryStatus.DRAFT)
    }

    @Test
    fun `create should reject when fiscal period is closed`() {
        val cashAccount =
            createMockAccount(
                id = java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"),
                code = "1000",
                name = "Cash",
                type = AccountType.ASSET,
                orgId = orgId,
            )
        val revenueAccount =
            createMockAccount(
                id = java.util.UUID.fromString("19770c7a-8b4f-3b6e-adb1-3631faff91ec"),
                code = "4000",
                name = "Revenue",
                type = AccountType.REVENUE,
                orgId = orgId,
            )

        `when`(
            accountRepository.findAllById(
                listOf(
                    java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"),
                    java.util.UUID.fromString("19770c7a-8b4f-3b6e-adb1-3631faff91ec"),
                ),
            ),
        ).thenReturn(listOf(cashAccount, revenueAccount))
        doThrow(BusinessRuleException("Fiscal period 'January 2026' is closed"))
            .`when`(fiscalYearService)
            .validatePeriodOpen(orgId, LocalDate.of(2026, 1, 15))

        val request =
            CreateJournalEntryRequest(
                date = LocalDate.of(2026, 1, 15),
                description = "Test entry",
                lines =
                    listOf(
                        JournalEntryLineRequest(
                            accountId = java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"),
                            debit = BigDecimal("100.00"),
                            credit = BigDecimal.ZERO,
                        ),
                        JournalEntryLineRequest(
                            accountId = java.util.UUID.fromString("19770c7a-8b4f-3b6e-adb1-3631faff91ec"),
                            debit = BigDecimal.ZERO,
                            credit = BigDecimal("100.00"),
                        ),
                    ),
            )

        val exception =
            assertThrows<BusinessRuleException> {
                journalEntryService.createJournalEntry(request, orgId, createdBy)
            }
        assertThat(exception.message).contains("closed")
    }

    @Test
    fun `create should succeed when fiscal period is open`() {
        val cashAccount =
            createMockAccount(
                id = java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"),
                code = "1000",
                name = "Cash",
                type = AccountType.ASSET,
                orgId = orgId,
            )
        val revenueAccount =
            createMockAccount(
                id = java.util.UUID.fromString("19770c7a-8b4f-3b6e-adb1-3631faff91ec"),
                code = "4000",
                name = "Revenue",
                type = AccountType.REVENUE,
                orgId = orgId,
            )
        `when`(
            accountRepository.findAllById(
                listOf(
                    java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"),
                    java.util.UUID.fromString("19770c7a-8b4f-3b6e-adb1-3631faff91ec"),
                ),
            ),
        ).thenReturn(listOf(cashAccount, revenueAccount))
        `when`(journalEntryRepository.countByOrganizationId(orgId)).thenReturn(0L)
        `when`(journalEntryRepository.save(any<JournalEntry>())).thenAnswer { it.arguments[0] }

        val request =
            CreateJournalEntryRequest(
                date = LocalDate.of(2026, 1, 15),
                description = "Test entry",
                lines =
                    listOf(
                        JournalEntryLineRequest(
                            accountId = java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"),
                            debit = BigDecimal("100.00"),
                            credit = BigDecimal.ZERO,
                        ),
                        JournalEntryLineRequest(
                            accountId = java.util.UUID.fromString("19770c7a-8b4f-3b6e-adb1-3631faff91ec"),
                            debit = BigDecimal.ZERO,
                            credit = BigDecimal("100.00"),
                        ),
                    ),
            )

        val result = journalEntryService.createJournalEntry(request, orgId, createdBy)
        assertThat(result.status).isEqualTo(JournalEntryStatus.DRAFT)
    }

    @Test
    fun `post should reject when fiscal period is closed`() {
        val entry =
            createMockEntry(
                id = java.util.UUID.fromString("883cd44c-a464-3674-a0ec-1fb21a7ccd22"),
                entryNumber = "JE-0001",
                status = JournalEntryStatus.DRAFT,
                lines =
                    listOf(
                        JournalEntryLine(
                            accountId = java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"),
                            accountCode = "1000",
                            accountName = "Cash",
                            debit = BigDecimal("100.00"),
                            credit = BigDecimal.ZERO,
                        ),
                        JournalEntryLine(
                            accountId = java.util.UUID.fromString("19770c7a-8b4f-3b6e-adb1-3631faff91ec"),
                            accountCode = "4000",
                            accountName = "Revenue",
                            debit = BigDecimal.ZERO,
                            credit = BigDecimal("100.00"),
                        ),
                    ),
                orgId = orgId,
            )

        `when`(
            journalEntryRepository.findById(java.util.UUID.fromString("883cd44c-a464-3674-a0ec-1fb21a7ccd22")),
        ).thenReturn(Optional.of(entry))
        doThrow(BusinessRuleException("Fiscal period 'January 2026' is closed"))
            .`when`(fiscalYearService)
            .validatePeriodOpen(orgId, LocalDate.of(2026, 1, 15))

        val exception =
            assertThrows<BusinessRuleException> {
                journalEntryService.postJournalEntry(java.util.UUID.fromString("883cd44c-a464-3674-a0ec-1fb21a7ccd22"), orgId)
            }
        assertThat(exception.message).contains("closed")
    }

    private fun createMockAccount(
        id: java.util.UUID = java.util.UUID.randomUUID(),
        code: String = "1000",
        name: String = "Cash",
        type: AccountType = AccountType.ASSET,
        orgId: UUID = java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"),
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
        id: UUID = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"),
        entryNumber: String = "JE-0001",
        status: JournalEntryStatus = JournalEntryStatus.DRAFT,
        lines: List<JournalEntryLine> = emptyList(),
        orgId: UUID = java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"),
    ) = JournalEntry(
        id = id,
        entryNumber = entryNumber,
        date = LocalDate.of(2026, 1, 15),
        description = "Test entry",
        organizationId = orgId,
        status = status,
        lines = lines,
        createdBy = java.util.UUID.fromString("1db2395f-13ba-3d37-9d2b-f77d3eb3aa2e"),
    )
}
