package com.froilan.synectix.service

import com.froilan.synectix.dto.AccountBalanceResponse
import com.froilan.synectix.dto.CreateJournalEntryRequest
import com.froilan.synectix.dto.TrialBalanceResponse
import com.froilan.synectix.model.AccountType
import com.froilan.synectix.model.FiscalPeriodStatus
import com.froilan.synectix.model.JournalEntry
import com.froilan.synectix.model.JournalEntryLine
import com.froilan.synectix.model.JournalEntrySource
import com.froilan.synectix.model.JournalEntryStatus
import com.froilan.synectix.repository.AccountRepository
import com.froilan.synectix.repository.JournalEntryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class JournalEntryService(
    private val journalEntryRepository: JournalEntryRepository,
    private val accountRepository: AccountRepository,
    private val fiscalYearService: FiscalYearService,
    private val entryNumberGenerator: JournalEntryNumberGenerator,
) {
    @Transactional
    fun createJournalEntry(
        request: CreateJournalEntryRequest,
        organizationId: String,
        createdBy: String,
    ): JournalEntry {
        if (request.lines.size < 2) {
            throw IllegalArgumentException("Journal entry must have at least 2 line items")
        }

        request.lines.forEach { line ->
            if (line.debit.compareTo(BigDecimal.ZERO) < 0 || line.credit.compareTo(BigDecimal.ZERO) < 0) {
                throw IllegalArgumentException("Debit and credit amounts must not be negative")
            }
            val hasDebit = line.debit.compareTo(BigDecimal.ZERO) > 0
            val hasCredit = line.credit.compareTo(BigDecimal.ZERO) > 0
            if (hasDebit && hasCredit) {
                throw IllegalArgumentException("A line item cannot have both debit and credit")
            }
            if (!hasDebit && !hasCredit) {
                throw IllegalArgumentException("A line item must have either a debit or credit amount")
            }
        }

        val totalDebits = request.lines.fold(BigDecimal.ZERO) { sum, line -> sum.add(line.debit) }
        val totalCredits = request.lines.fold(BigDecimal.ZERO) { sum, line -> sum.add(line.credit) }
        if (totalDebits.compareTo(totalCredits) != 0) {
            throw IllegalArgumentException(
                "Journal entry must balance: debits ($totalDebits) != credits ($totalCredits)",
            )
        }

        val accountIds = request.lines.map { it.accountId }.distinct()
        val accounts = accountRepository.findAllById(accountIds).associateBy { it.id }

        val lines =
            request.lines.map { line ->
                val account =
                    accounts[line.accountId]
                        ?: throw IllegalArgumentException("Account '${line.accountId}' not found")
                if (account.organizationId != organizationId) {
                    throw IllegalArgumentException("Account '${line.accountId}' not found")
                }
                if (!account.isActive) {
                    throw IllegalArgumentException("Account '${account.code}' is inactive")
                }
                JournalEntryLine(
                    accountId = account.id,
                    accountCode = account.code,
                    accountName = account.name,
                    debit = line.debit,
                    credit = line.credit,
                    description = line.description,
                )
            }

        validateFiscalPeriodOpen(organizationId, request.date)

        return entryNumberGenerator.saveWithRetry(organizationId) { entryNumber ->
            JournalEntry(
                entryNumber = entryNumber,
                date = request.date,
                description = request.description,
                organizationId = organizationId,
                lines = lines,
                createdBy = createdBy,
                sourceReference = request.sourceReference,
            )
        }
    }

    @Transactional
    fun postJournalEntry(
        entryId: String,
        organizationId: String,
    ): JournalEntry {
        val entry = findEntry(entryId, organizationId)
        if (entry.status != JournalEntryStatus.DRAFT) {
            throw IllegalArgumentException("Only draft entries can be posted")
        }

        val totalDebits = entry.lines.fold(BigDecimal.ZERO) { sum, line -> sum.add(line.debit) }
        val totalCredits = entry.lines.fold(BigDecimal.ZERO) { sum, line -> sum.add(line.credit) }
        if (totalDebits.compareTo(totalCredits) != 0) {
            throw IllegalArgumentException("Journal entry is not balanced")
        }

        validateFiscalPeriodOpen(organizationId, entry.date)

        return journalEntryRepository.save(
            entry.copy(
                status = JournalEntryStatus.POSTED,
                postedAt = LocalDateTime.now(ZoneOffset.UTC),
            ),
        )
    }

    @Transactional
    fun voidJournalEntry(
        entryId: String,
        organizationId: String,
        reason: String,
    ): JournalEntry {
        val entry = findEntry(entryId, organizationId)
        if (entry.status != JournalEntryStatus.POSTED) {
            throw IllegalArgumentException("Only posted entries can be voided")
        }

        val voidedEntry =
            journalEntryRepository.save(
                entry.copy(
                    status = JournalEntryStatus.VOIDED,
                    voidedAt = LocalDateTime.now(ZoneOffset.UTC),
                    voidReason = reason,
                ),
            )

        val reversedLines =
            entry.lines.map { line ->
                line.copy(debit = line.credit, credit = line.debit)
            }

        entryNumberGenerator.saveWithRetry(organizationId) { reversingNumber ->
            JournalEntry(
                entryNumber = reversingNumber,
                date = LocalDate.now(),
                description = "Reversal of ${entry.entryNumber}: $reason",
                organizationId = organizationId,
                status = JournalEntryStatus.POSTED,
                source = JournalEntrySource.SYSTEM,
                sourceReference = "VOID-${entry.id}",
                lines = reversedLines,
                createdBy = entry.createdBy,
                postedAt = LocalDateTime.now(ZoneOffset.UTC),
            )
        }

        return voidedEntry
    }

    fun getJournalEntry(
        entryId: String,
        organizationId: String,
    ): JournalEntry = findEntry(entryId, organizationId)

    fun listJournalEntries(
        organizationId: String,
        status: JournalEntryStatus? = null,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
    ): List<JournalEntry> =
        when {
            status != null && startDate != null && endDate != null ->
                journalEntryRepository.findByOrganizationIdAndStatusAndDateBetween(organizationId, status, startDate, endDate)
            status != null && startDate != null ->
                journalEntryRepository.findByOrganizationIdAndStatusAndDateGreaterThanEqual(organizationId, status, startDate)
            status != null && endDate != null ->
                journalEntryRepository.findByOrganizationIdAndStatusAndDateLessThanEqual(organizationId, status, endDate)
            status != null ->
                journalEntryRepository.findByOrganizationIdAndStatus(organizationId, status)
            startDate != null && endDate != null ->
                journalEntryRepository.findByOrganizationIdAndDateBetween(organizationId, startDate, endDate)
            startDate != null ->
                journalEntryRepository.findByOrganizationIdAndDateGreaterThanEqual(organizationId, startDate)
            endDate != null ->
                journalEntryRepository.findByOrganizationIdAndDateLessThanEqual(organizationId, endDate)
            else ->
                journalEntryRepository.findByOrganizationId(organizationId)
        }

    fun getAccountBalance(
        accountId: String,
        organizationId: String,
        asOfDate: LocalDate? = null,
    ): AccountBalanceResponse {
        val account =
            accountRepository.findById(accountId).orElseThrow {
                IllegalArgumentException("Account not found")
            }
        if (account.organizationId != organizationId) {
            throw IllegalArgumentException("Account not found")
        }

        val postedStatuses = listOf(JournalEntryStatus.POSTED, JournalEntryStatus.VOIDED)
        val entries =
            if (asOfDate != null) {
                journalEntryRepository.findByOrganizationIdAndStatusInAndDateLessThanEqual(
                    organizationId,
                    postedStatuses,
                    asOfDate,
                )
            } else {
                journalEntryRepository.findByOrganizationIdAndStatusIn(
                    organizationId,
                    postedStatuses,
                )
            }

        var totalDebits = BigDecimal.ZERO
        var totalCredits = BigDecimal.ZERO
        entries.forEach { entry ->
            entry.lines.filter { it.accountId == accountId }.forEach { line ->
                totalDebits = totalDebits.add(line.debit)
                totalCredits = totalCredits.add(line.credit)
            }
        }

        val balance =
            when (account.type) {
                AccountType.ASSET, AccountType.EXPENSE -> totalDebits.subtract(totalCredits)
                AccountType.LIABILITY, AccountType.EQUITY, AccountType.REVENUE -> totalCredits.subtract(totalDebits)
            }

        return AccountBalanceResponse(
            accountId = account.id,
            accountCode = account.code,
            accountName = account.name,
            accountType = account.type.name,
            totalDebits = totalDebits,
            totalCredits = totalCredits,
            balance = balance,
        )
    }

    fun getTrialBalance(
        organizationId: String,
        asOfDate: LocalDate? = null,
    ): TrialBalanceResponse {
        val postedStatuses = listOf(JournalEntryStatus.POSTED, JournalEntryStatus.VOIDED)
        val entries =
            if (asOfDate != null) {
                journalEntryRepository.findByOrganizationIdAndStatusInAndDateLessThanEqual(
                    organizationId,
                    postedStatuses,
                    asOfDate,
                )
            } else {
                journalEntryRepository.findByOrganizationIdAndStatusIn(
                    organizationId,
                    postedStatuses,
                )
            }

        val accountTotals = mutableMapOf<String, Pair<BigDecimal, BigDecimal>>()
        entries.forEach { entry ->
            entry.lines.forEach { line ->
                val (debits, credits) = accountTotals.getOrDefault(line.accountId, BigDecimal.ZERO to BigDecimal.ZERO)
                accountTotals[line.accountId] = debits.add(line.debit) to credits.add(line.credit)
            }
        }

        val allAccounts = accountRepository.findByOrganizationIdAndIsActive(organizationId, true)

        val accountBalances =
            allAccounts
                .map { account ->
                    val (totalDebits, totalCredits) =
                        accountTotals.getOrDefault(account.id, BigDecimal.ZERO to BigDecimal.ZERO)
                    val balance =
                        when (account.type) {
                            AccountType.ASSET, AccountType.EXPENSE -> totalDebits.subtract(totalCredits)
                            AccountType.LIABILITY, AccountType.EQUITY, AccountType.REVENUE -> totalCredits.subtract(totalDebits)
                        }
                    AccountBalanceResponse(
                        accountId = account.id,
                        accountCode = account.code,
                        accountName = account.name,
                        accountType = account.type.name,
                        totalDebits = totalDebits,
                        totalCredits = totalCredits,
                        balance = balance,
                    )
                }.sortedBy { it.accountCode }

        val grandTotalDebits = accountBalances.fold(BigDecimal.ZERO) { sum, ab -> sum.add(ab.totalDebits) }
        val grandTotalCredits = accountBalances.fold(BigDecimal.ZERO) { sum, ab -> sum.add(ab.totalCredits) }

        return TrialBalanceResponse(
            accounts = accountBalances,
            totalDebits = grandTotalDebits,
            totalCredits = grandTotalCredits,
            asOfDate = asOfDate?.toString(),
        )
    }

    private fun findEntry(
        entryId: String,
        organizationId: String,
    ): JournalEntry {
        val entry =
            journalEntryRepository.findById(entryId).orElseThrow {
                IllegalArgumentException("Journal entry not found")
            }
        if (entry.organizationId != organizationId) {
            throw IllegalArgumentException("Journal entry not found")
        }
        return entry
    }

    private fun validateFiscalPeriodOpen(
        organizationId: String,
        date: LocalDate,
    ) {
        when (val result = fiscalYearService.findPeriodForDate(organizationId, date)) {
            is FiscalYearService.PeriodLookupResult.NoFiscalYears -> return
            is FiscalYearService.PeriodLookupResult.NotFound ->
                throw IllegalArgumentException("No fiscal period covers the date $date")
            is FiscalYearService.PeriodLookupResult.Found -> {
                if (result.period.status == FiscalPeriodStatus.CLOSED) {
                    throw IllegalArgumentException("Fiscal period '${result.period.name}' is closed")
                }
            }
        }
    }
}
