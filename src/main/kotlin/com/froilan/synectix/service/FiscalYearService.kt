package com.froilan.synectix.service

import com.froilan.synectix.exception.ResourceNotFoundException

import com.froilan.synectix.dto.CreateFiscalYearRequest
import com.froilan.synectix.model.AccountType
import com.froilan.synectix.model.FiscalPeriod
import com.froilan.synectix.model.FiscalPeriodStatus
import com.froilan.synectix.model.FiscalYear
import com.froilan.synectix.model.FiscalYearStatus
import com.froilan.synectix.model.JournalEntry
import com.froilan.synectix.model.JournalEntryLine
import com.froilan.synectix.model.JournalEntrySource
import com.froilan.synectix.model.JournalEntryStatus
import com.froilan.synectix.repository.AccountRepository
import com.froilan.synectix.repository.FiscalYearRepository
import com.froilan.synectix.repository.JournalEntryRepository
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.util.Locale

@Service
class FiscalYearService(
    private val fiscalYearRepository: FiscalYearRepository,
    private val journalEntryRepository: JournalEntryRepository,
    private val accountRepository: AccountRepository,
    private val entryNumberGenerator: JournalEntryNumberGenerator,
) {
    @Transactional
    fun createFiscalYear(
        request: CreateFiscalYearRequest,
        organizationId: String,
    ): FiscalYear {
        if (!request.startDate.isBefore(request.endDate)) {
            throw IllegalArgumentException("Start date must be before end date")
        }

        val existing = fiscalYearRepository.findByOrganizationId(organizationId)

        val activeFiscalYear = existing.firstOrNull { it.status == FiscalYearStatus.ACTIVE }
        if (activeFiscalYear != null) {
            throw IllegalArgumentException(
                "An active fiscal year '${activeFiscalYear.name}' already exists in this organization",
            )
        }

        existing.forEach { fy ->
            if (!request.startDate.isAfter(fy.endDate) && !request.endDate.isBefore(fy.startDate)) {
                throw IllegalArgumentException(
                    "Date range overlaps with existing fiscal year '${fy.name}'",
                )
            }
        }

        val periods = generateMonthlyPeriods(request.startDate, request.endDate)

        val fiscalYear =
            FiscalYear(
                name = request.name,
                startDate = request.startDate,
                endDate = request.endDate,
                organizationId = organizationId,
                periods = periods,
            )

        return try {
            fiscalYearRepository.save(fiscalYear)
        } catch (e: DuplicateKeyException) {
            throw IllegalArgumentException(
                "Fiscal year '${request.name}' already exists in this organization",
                e,
            )
        }
    }

    fun getFiscalYear(
        fiscalYearId: String,
        organizationId: String,
    ): FiscalYear = findFiscalYear(fiscalYearId, organizationId)

    fun listFiscalYears(organizationId: String): List<FiscalYear> = fiscalYearRepository.findByOrganizationId(organizationId)

    @Transactional
    fun closePeriod(
        fiscalYearId: String,
        periodId: String,
        organizationId: String,
        closedBy: String,
    ): FiscalYear {
        val fiscalYear = findFiscalYear(fiscalYearId, organizationId)

        if (fiscalYear.status == FiscalYearStatus.CLOSED) {
            throw IllegalArgumentException("Fiscal year is already closed")
        }

        val periodIndex = fiscalYear.periods.indexOfFirst { it.id == periodId }
        if (periodIndex == -1) {
            throw ResourceNotFoundException("Fiscal period not found")
        }

        val period = fiscalYear.periods[periodIndex]
        if (period.status == FiscalPeriodStatus.CLOSED) {
            throw IllegalArgumentException("Fiscal period is already closed")
        }

        val precedingOpen =
            fiscalYear.periods
                .take(periodIndex)
                .any { it.status != FiscalPeriodStatus.CLOSED }
        if (precedingOpen) {
            throw IllegalArgumentException("All preceding periods must be closed first")
        }

        val updatedPeriods = fiscalYear.periods.toMutableList()
        updatedPeriods[periodIndex] =
            period.copy(
                status = FiscalPeriodStatus.CLOSED,
                closedAt = LocalDateTime.now(ZoneOffset.UTC),
                closedBy = closedBy,
            )

        return fiscalYearRepository.save(
            fiscalYear.copy(periods = updatedPeriods),
        )
    }

    @Transactional
    fun reopenPeriod(
        fiscalYearId: String,
        periodId: String,
        organizationId: String,
        reopenedBy: String,
    ): FiscalYear {
        val fiscalYear = findFiscalYear(fiscalYearId, organizationId)

        if (fiscalYear.status == FiscalYearStatus.CLOSED) {
            throw IllegalArgumentException("Cannot reopen period in a closed fiscal year")
        }

        val periodIndex = fiscalYear.periods.indexOfFirst { it.id == periodId }
        if (periodIndex == -1) {
            throw ResourceNotFoundException("Fiscal period not found")
        }

        val period = fiscalYear.periods[periodIndex]
        if (period.status != FiscalPeriodStatus.CLOSED) {
            throw IllegalArgumentException("Only closed periods can be reopened")
        }

        val subsequentOpen =
            fiscalYear.periods
                .drop(periodIndex + 1)
                .any { it.status != FiscalPeriodStatus.CLOSED }
        if (subsequentOpen) {
            throw IllegalArgumentException("All subsequent periods must be closed first")
        }

        val updatedPeriods = fiscalYear.periods.toMutableList()
        updatedPeriods[periodIndex] =
            period.copy(
                status = FiscalPeriodStatus.REOPENED,
                reopenedAt = LocalDateTime.now(ZoneOffset.UTC),
                reopenedBy = reopenedBy,
            )

        return fiscalYearRepository.save(
            fiscalYear.copy(periods = updatedPeriods),
        )
    }

    @Transactional
    fun closeYear(
        fiscalYearId: String,
        organizationId: String,
        closedBy: String,
    ): FiscalYear {
        val fiscalYear = findFiscalYear(fiscalYearId, organizationId)

        if (fiscalYear.status == FiscalYearStatus.CLOSED) {
            throw IllegalArgumentException("Fiscal year is already closed")
        }

        val allPeriodsClosed = fiscalYear.periods.all { it.status == FiscalPeriodStatus.CLOSED }
        if (!allPeriodsClosed) {
            throw IllegalArgumentException("All periods must be closed before closing the fiscal year")
        }

        val closingEntry = createClosingEntry(fiscalYear, organizationId, closedBy)

        return fiscalYearRepository.save(
            fiscalYear.copy(
                status = FiscalYearStatus.CLOSED,
                closedAt = LocalDateTime.now(ZoneOffset.UTC),
                closedBy = closedBy,
                closingEntryId = closingEntry?.id,
            ),
        )
    }

    fun findPeriodForDate(
        organizationId: String,
        date: LocalDate,
    ): PeriodLookupResult {
        val allFiscalYears = fiscalYearRepository.findByOrganizationId(organizationId)
        if (allFiscalYears.isEmpty()) return PeriodLookupResult.NoFiscalYears

        for (fy in allFiscalYears) {
            for (period in fy.periods) {
                if (!date.isBefore(period.startDate) && !date.isAfter(period.endDate)) {
                    return PeriodLookupResult.Found(period)
                }
            }
        }
        return PeriodLookupResult.NotFound
    }

    fun validatePeriodOpen(
        organizationId: String,
        date: LocalDate,
    ) {
        when (val result = findPeriodForDate(organizationId, date)) {
            is PeriodLookupResult.NoFiscalYears -> return
            is PeriodLookupResult.NotFound ->
                throw IllegalArgumentException("No fiscal period covers the date $date")
            is PeriodLookupResult.Found -> {
                if (result.period.status == FiscalPeriodStatus.CLOSED) {
                    throw IllegalArgumentException("Fiscal period '${result.period.name}' is closed")
                }
            }
        }
    }

    sealed class PeriodLookupResult {
        data object NoFiscalYears : PeriodLookupResult()

        data object NotFound : PeriodLookupResult()

        data class Found(
            val period: FiscalPeriod,
        ) : PeriodLookupResult()
    }

    private fun createClosingEntry(
        fiscalYear: FiscalYear,
        organizationId: String,
        closedBy: String,
    ): JournalEntry? {
        val sourceRef = "YEAR-END-CLOSE-${fiscalYear.id}"
        if (journalEntryRepository.existsByOrganizationIdAndSourceReference(organizationId, sourceRef)) {
            throw IllegalArgumentException("Year-end closing entry already exists for this fiscal year")
        }

        val postedStatuses = listOf(JournalEntryStatus.POSTED, JournalEntryStatus.VOIDED)
        val entries =
            journalEntryRepository.findByOrganizationIdAndStatusInAndDateBetween(
                organizationId,
                postedStatuses,
                fiscalYear.startDate,
                fiscalYear.endDate,
            )

        val accountTotals = mutableMapOf<String, Pair<BigDecimal, BigDecimal>>()
        entries.forEach { entry ->
            entry.lines.forEach { line ->
                val (debits, credits) =
                    accountTotals.getOrDefault(
                        line.accountId,
                        BigDecimal.ZERO to BigDecimal.ZERO,
                    )
                accountTotals[line.accountId] = debits.add(line.debit) to credits.add(line.credit)
            }
        }

        val accounts =
            accountRepository
                .findAllById(accountTotals.keys)
                .associateBy { it.id }

        val missingAccountIds = accountTotals.keys - accounts.keys
        if (missingAccountIds.isNotEmpty()) {
            throw IllegalStateException(
                "Cannot create closing entry: missing accounts ${missingAccountIds.sorted().joinToString(", ")}",
            )
        }

        val closingLines = mutableListOf<JournalEntryLine>()

        accountTotals.forEach { (accountId, totals) ->
            val account = accounts.getValue(accountId)
            val (totalDebits, totalCredits) = totals

            when (account.type) {
                AccountType.REVENUE -> {
                    val balance = totalCredits.subtract(totalDebits)
                    if (balance.compareTo(BigDecimal.ZERO) != 0) {
                        closingLines.add(
                            JournalEntryLine(
                                accountId = account.id,
                                accountCode = account.code,
                                accountName = account.name,
                                debit = if (balance > BigDecimal.ZERO) balance else BigDecimal.ZERO,
                                credit = if (balance < BigDecimal.ZERO) balance.negate() else BigDecimal.ZERO,
                            ),
                        )
                    }
                }
                AccountType.EXPENSE -> {
                    val balance = totalDebits.subtract(totalCredits)
                    if (balance.compareTo(BigDecimal.ZERO) != 0) {
                        closingLines.add(
                            JournalEntryLine(
                                accountId = account.id,
                                accountCode = account.code,
                                accountName = account.name,
                                debit = if (balance < BigDecimal.ZERO) balance.negate() else BigDecimal.ZERO,
                                credit = if (balance > BigDecimal.ZERO) balance else BigDecimal.ZERO,
                            ),
                        )
                    }
                }
                else -> {}
            }
        }

        if (closingLines.isEmpty()) return null

        val totalClosingDebits = closingLines.fold(BigDecimal.ZERO) { sum, l -> sum.add(l.debit) }
        val totalClosingCredits = closingLines.fold(BigDecimal.ZERO) { sum, l -> sum.add(l.credit) }
        val difference = totalClosingDebits.subtract(totalClosingCredits)

        if (difference.compareTo(BigDecimal.ZERO) != 0) {
            val retainedEarnings =
                accountRepository
                    .findByOrganizationIdAndCode(organizationId, "3100")
                    .orElseThrow {
                        IllegalStateException("Retained Earnings account (3100) not found")
                    }

            closingLines.add(
                JournalEntryLine(
                    accountId = retainedEarnings.id,
                    accountCode = retainedEarnings.code,
                    accountName = retainedEarnings.name,
                    debit = if (difference < BigDecimal.ZERO) difference.negate() else BigDecimal.ZERO,
                    credit = if (difference > BigDecimal.ZERO) difference else BigDecimal.ZERO,
                ),
            )
        }

        return entryNumberGenerator.saveWithRetry(organizationId) { entryNumber ->
            JournalEntry(
                entryNumber = entryNumber,
                date = fiscalYear.endDate,
                description = "Year-end closing entry for ${fiscalYear.name}",
                organizationId = organizationId,
                status = JournalEntryStatus.POSTED,
                source = JournalEntrySource.SYSTEM,
                sourceReference = sourceRef,
                lines = closingLines,
                createdBy = closedBy,
                postedAt = LocalDateTime.now(ZoneOffset.UTC),
            )
        }
    }

    private fun generateMonthlyPeriods(
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<FiscalPeriod> {
        val periods = mutableListOf<FiscalPeriod>()
        var periodStart = startDate
        var periodNumber = 1

        while (periodStart.isBefore(endDate) || periodStart.isEqual(endDate)) {
            val monthEnd = periodStart.withDayOfMonth(periodStart.lengthOfMonth())
            val periodEnd = if (monthEnd.isAfter(endDate)) endDate else monthEnd

            val monthName = periodStart.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
            val name = "$monthName ${periodStart.year}"

            periods.add(
                FiscalPeriod(
                    periodNumber = periodNumber,
                    name = name,
                    startDate = periodStart,
                    endDate = periodEnd,
                ),
            )

            periodStart = periodEnd.plusDays(1)
            periodNumber++
        }

        return periods
    }

    private fun findFiscalYear(
        fiscalYearId: String,
        organizationId: String,
    ): FiscalYear {
        val fiscalYear =
            fiscalYearRepository.findById(fiscalYearId).orElseThrow {
                ResourceNotFoundException("Fiscal year not found")
            }
        if (fiscalYear.organizationId != organizationId) {
            throw ResourceNotFoundException("Fiscal year not found")
        }
        return fiscalYear
    }
}
