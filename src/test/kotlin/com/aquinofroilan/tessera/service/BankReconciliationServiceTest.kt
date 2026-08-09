package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.model.BankAccount
import com.aquinofroilan.tessera.model.BankStatement
import com.aquinofroilan.tessera.model.BankStatementLine
import com.aquinofroilan.tessera.model.JournalEntry
import com.aquinofroilan.tessera.model.JournalEntryLine
import com.aquinofroilan.tessera.model.JournalEntryStatus
import com.aquinofroilan.tessera.repository.BankStatementRepository
import com.aquinofroilan.tessera.repository.JournalEntryRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

class BankReconciliationServiceTest {
    private lateinit var statementRepository: BankStatementRepository
    private lateinit var bankAccountService: BankAccountService
    private lateinit var journalEntryRepository: JournalEntryRepository
    private lateinit var service: BankReconciliationService

    private val orgId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000100")
    private val userId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000101")
    private val bankId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000102")
    private val glAccountId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000103")

    @BeforeEach
    fun setup() {
        statementRepository = mock(BankStatementRepository::class.java)
        bankAccountService = mock(BankAccountService::class.java)
        journalEntryRepository = mock(JournalEntryRepository::class.java)
        whenever(statementRepository.save(any<BankStatement>())).thenAnswer { it.arguments[0] }
        whenever(bankAccountService.getBankAccount(bankId, orgId)).thenReturn(bankAccount())
        service = BankReconciliationService(statementRepository, bankAccountService, journalEntryRepository)
    }

    @Test
    fun `auto-match links a single candidate JE within drift window`() {
        val stmt =
            statement(
                listOf(
                    line(java.util.UUID.fromString("00000000-0000-0000-0000-000000000104"), BigDecimal("100"), LocalDate.of(2026, 6, 5)),
                ),
            )
        whenever(
            statementRepository.findById(java.util.UUID.fromString("00000000-0000-0000-0000-000000000108")),
        ).thenReturn(Optional.of(stmt))
        whenever(
            journalEntryRepository.findByOrganizationIdAndStatusAndDateBetween(any(), any(), any(), any()),
        ).thenReturn(
            listOf(
                journalEntry(
                    java.util.UUID.fromString("00000000-0000-0000-0000-000000000106"),
                    LocalDate.of(2026, 6, 4),
                    debit = BigDecimal("100"),
                    credit = BigDecimal.ZERO,
                ),
            ),
        )

        val result =
            service.autoMatch(
                java.util.UUID.fromString("00000000-0000-0000-0000-000000000108"),
                orgId,
                userId,
                maxDateDriftDays = 5,
            )

        assertThat(result.matched).hasSize(1)
        assertThat(result.matched[0].journalEntryId).isEqualTo(java.util.UUID.fromString("00000000-0000-0000-0000-000000000106"))
        assertThat(result.unmatchedLineIds).isEmpty()
        assertThat(result.ambiguousLineIds).isEmpty()
    }

    @Test
    fun `auto-match flags ambiguous when multiple JEs match`() {
        val stmt =
            statement(
                listOf(
                    line(java.util.UUID.fromString("00000000-0000-0000-0000-000000000104"), BigDecimal("100"), LocalDate.of(2026, 6, 5)),
                ),
            )
        whenever(
            statementRepository.findById(java.util.UUID.fromString("00000000-0000-0000-0000-000000000108")),
        ).thenReturn(Optional.of(stmt))
        whenever(
            journalEntryRepository.findByOrganizationIdAndStatusAndDateBetween(any(), any(), any(), any()),
        ).thenReturn(
            listOf(
                journalEntry(
                    java.util.UUID.fromString("00000000-0000-0000-0000-000000000106"),
                    LocalDate.of(2026, 6, 4),
                    debit = BigDecimal("100"),
                    credit = BigDecimal.ZERO,
                ),
                journalEntry(
                    java.util.UUID.fromString("00000000-0000-0000-0000-000000000107"),
                    LocalDate.of(2026, 6, 6),
                    debit = BigDecimal("100"),
                    credit = BigDecimal.ZERO,
                ),
            ),
        )

        val result =
            service.autoMatch(
                java.util.UUID.fromString("00000000-0000-0000-0000-000000000108"),
                orgId,
                userId,
                maxDateDriftDays = 5,
            )

        assertThat(result.matched).isEmpty()
        assertThat(result.ambiguousLineIds).containsExactly(java.util.UUID.fromString("00000000-0000-0000-0000-000000000104"))
    }

    @Test
    fun `auto-match unmatched when no candidate amount agrees`() {
        val stmt =
            statement(
                listOf(
                    line(java.util.UUID.fromString("00000000-0000-0000-0000-000000000104"), BigDecimal("100"), LocalDate.of(2026, 6, 5)),
                ),
            )
        whenever(
            statementRepository.findById(java.util.UUID.fromString("00000000-0000-0000-0000-000000000108")),
        ).thenReturn(Optional.of(stmt))
        whenever(
            journalEntryRepository.findByOrganizationIdAndStatusAndDateBetween(any(), any(), any(), any()),
        ).thenReturn(
            listOf(
                journalEntry(
                    java.util.UUID.fromString("00000000-0000-0000-0000-000000000106"),
                    LocalDate.of(2026, 6, 4),
                    debit = BigDecimal("99"),
                    credit = BigDecimal.ZERO,
                ),
            ),
        )

        val result =
            service.autoMatch(
                java.util.UUID.fromString("00000000-0000-0000-0000-000000000108"),
                orgId,
                userId,
                maxDateDriftDays = 5,
            )

        assertThat(result.unmatchedLineIds).containsExactly(java.util.UUID.fromString("00000000-0000-0000-0000-000000000104"))
    }

    @Test
    fun `manual-match rejects an amount mismatch`() {
        val stmt =
            statement(
                listOf(
                    line(java.util.UUID.fromString("00000000-0000-0000-0000-000000000104"), BigDecimal("100"), LocalDate.of(2026, 6, 5)),
                ),
            )
        whenever(
            statementRepository.findById(java.util.UUID.fromString("00000000-0000-0000-0000-000000000108")),
        ).thenReturn(Optional.of(stmt))
        whenever(journalEntryRepository.findById(java.util.UUID.fromString("00000000-0000-0000-0000-000000000106"))).thenReturn(
            Optional.of(
                journalEntry(
                    java.util.UUID.fromString("00000000-0000-0000-0000-000000000106"),
                    LocalDate.of(2026, 6, 5),
                    debit = BigDecimal("50"),
                    credit = BigDecimal.ZERO,
                ),
            ),
        )
        assertThatThrownBy {
            service.manualMatch(
                java.util.UUID.fromString("00000000-0000-0000-0000-000000000108"),
                java.util.UUID.fromString("00000000-0000-0000-0000-000000000104"),
                java.util.UUID.fromString("00000000-0000-0000-0000-000000000106"),
                orgId,
                userId,
            )
        }.isInstanceOf(BusinessRuleException::class.java)
            .hasMessageContaining("Amount mismatch")
    }

    @Test
    fun `manual-match rejects non-POSTED journal entry`() {
        val stmt =
            statement(
                listOf(
                    line(java.util.UUID.fromString("00000000-0000-0000-0000-000000000104"), BigDecimal("100"), LocalDate.of(2026, 6, 5)),
                ),
            )
        whenever(
            statementRepository.findById(java.util.UUID.fromString("00000000-0000-0000-0000-000000000108")),
        ).thenReturn(Optional.of(stmt))
        whenever(journalEntryRepository.findById(java.util.UUID.fromString("00000000-0000-0000-0000-000000000106"))).thenReturn(
            Optional.of(
                journalEntry(
                    java.util.UUID.fromString("00000000-0000-0000-0000-000000000106"),
                    LocalDate.of(2026, 6, 5),
                    debit = BigDecimal("100"),
                    credit = BigDecimal.ZERO,
                    status = JournalEntryStatus.DRAFT,
                ),
            ),
        )
        assertThatThrownBy {
            service.manualMatch(
                java.util.UUID.fromString("00000000-0000-0000-0000-000000000108"),
                java.util.UUID.fromString("00000000-0000-0000-0000-000000000104"),
                java.util.UUID.fromString("00000000-0000-0000-0000-000000000106"),
                orgId,
                userId,
            )
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `unmatch clears reconciliation fields`() {
        val matched =
            line(java.util.UUID.fromString("00000000-0000-0000-0000-000000000104"), BigDecimal("100"), LocalDate.of(2026, 6, 5)).copy(
                reconciled = true,
                reconciledJournalEntryId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000106"),
            )
        val stmt = statement(listOf(matched))
        whenever(
            statementRepository.findById(java.util.UUID.fromString("00000000-0000-0000-0000-000000000108")),
        ).thenReturn(Optional.of(stmt))

        val updated =
            service.unmatch(
                java.util.UUID.fromString("00000000-0000-0000-0000-000000000108"),
                java.util.UUID.fromString("00000000-0000-0000-0000-000000000104"),
                orgId,
            )
        val cleared = updated.lines.first { it.id == java.util.UUID.fromString("00000000-0000-0000-0000-000000000104") }
        assertThat(cleared.reconciled).isFalse
        assertThat(cleared.reconciledJournalEntryId).isNull()
    }

    private fun bankAccount() =
        BankAccount(
            id = bankId,
            organizationId = orgId,
            code = "MAIN",
            name = "Main",
            currency = "USD",
            glAccountId = glAccountId,
            isActive = true,
            createdBy = userId,
        )

    private fun statement(lines: List<BankStatementLine>) =
        BankStatement(
            id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000108"),
            organizationId = orgId,
            bankAccountId = bankId,
            statementDate = LocalDate.of(2026, 6, 30),
            openingBalance = BigDecimal.ZERO,
            closingBalance = lines.fold(BigDecimal.ZERO) { acc, l -> acc.add(l.amount) },
            currency = "USD",
            uploadedBy = userId,
            lines = lines,
        )

    private fun line(
        id: java.util.UUID,
        amount: BigDecimal,
        date: LocalDate,
    ) = BankStatementLine(
        id = id,
        lineNumber = 1,
        postedDate = date,
        description = "Transaction",
        amount = amount,
    )

    private fun journalEntry(
        id: java.util.UUID,
        date: LocalDate,
        debit: BigDecimal,
        credit: BigDecimal,
        status: JournalEntryStatus = JournalEntryStatus.POSTED,
    ) = JournalEntry(
        id = id,
        entryNumber = "JE-$id",
        date = date,
        description = "Auto",
        organizationId = orgId,
        status = status,
        lines =
            listOf(
                JournalEntryLine(
                    accountId = glAccountId,
                    accountCode = "1000",
                    accountName = "Cash",
                    debit = debit,
                    credit = credit,
                ),
                JournalEntryLine(
                    accountId = java.util.UUID.randomUUID(),
                    accountCode = "4000",
                    accountName = "Revenue",
                    debit = credit,
                    credit = debit,
                ),
            ),
        createdBy = userId,
    )
}
