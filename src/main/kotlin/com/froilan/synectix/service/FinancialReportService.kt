package com.froilan.synectix.service

import com.froilan.synectix.model.AccountType
import com.froilan.synectix.model.JournalEntryStatus
import com.froilan.synectix.repository.JournalEntryRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

@Service
class FinancialReportService(
    private val journalEntryRepository: JournalEntryRepository,
) {
    internal fun computePeriodTotals(
        organizationId: String,
        startDate: LocalDate?,
        endDate: LocalDate,
    ): Map<String, Pair<BigDecimal, BigDecimal>> {
        val postedStatuses = listOf(JournalEntryStatus.POSTED, JournalEntryStatus.VOIDED)
        val entries =
            if (startDate != null) {
                journalEntryRepository.findByOrganizationIdAndStatusInAndDateBetween(
                    organizationId,
                    postedStatuses,
                    startDate,
                    endDate,
                )
            } else {
                journalEntryRepository.findByOrganizationIdAndStatusInAndDateLessThanEqual(
                    organizationId,
                    postedStatuses,
                    endDate,
                )
            }

        val totals = mutableMapOf<String, Pair<BigDecimal, BigDecimal>>()
        entries.forEach { entry ->
            entry.lines.forEach { line ->
                val (debits, credits) = totals.getOrDefault(line.accountId, BigDecimal.ZERO to BigDecimal.ZERO)
                totals[line.accountId] = debits.add(line.debit) to credits.add(line.credit)
            }
        }
        return totals
    }

    internal fun signedBalance(
        type: AccountType,
        debits: BigDecimal,
        credits: BigDecimal,
    ): BigDecimal =
        when (type) {
            AccountType.ASSET, AccountType.EXPENSE -> debits.subtract(credits)
            AccountType.LIABILITY, AccountType.EQUITY, AccountType.REVENUE -> credits.subtract(debits)
        }
}
