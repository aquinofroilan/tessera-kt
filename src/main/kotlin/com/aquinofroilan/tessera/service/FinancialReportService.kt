package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.BalanceSheetResponse
import com.aquinofroilan.tessera.dto.ComparativePeriodMeta
import com.aquinofroilan.tessera.dto.ComparativeTrialBalanceResponse
import com.aquinofroilan.tessera.dto.IncomeStatementResponse
import com.aquinofroilan.tessera.dto.ReportAccountLine
import com.aquinofroilan.tessera.dto.SyntheticAccountIds
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.model.Account
import com.aquinofroilan.tessera.model.AccountType
import com.aquinofroilan.tessera.repository.AccountRepository
import com.aquinofroilan.tessera.repository.JournalEntryRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

@Service
class FinancialReportService(
    private val journalEntryRepository: JournalEntryRepository,
    private val accountRepository: AccountRepository,
    private val journalEntryService: JournalEntryService,
) {
    fun getComparativeTrialBalance(
        organizationId: java.util.UUID,
        asOfDate: LocalDate?,
        compareAsOfDate: LocalDate?,
    ) = ComparativeTrialBalanceResponse(
        current = journalEntryService.getTrialBalance(organizationId, asOfDate),
        comparative = compareAsOfDate?.let { journalEntryService.getTrialBalance(organizationId, it) },
    )

    fun getIncomeStatement(
        organizationId: java.util.UUID,
        startDate: LocalDate,
        endDate: LocalDate,
        compareStartDate: LocalDate? = null,
        compareEndDate: LocalDate? = null,
    ): IncomeStatementResponse {
        if (endDate.isBefore(startDate)) {
            throw BusinessRuleException("End date must be on or after start date")
        }
        if ((compareStartDate == null) != (compareEndDate == null)) {
            throw BusinessRuleException("Both comparative dates must be provided together")
        }
        if (compareStartDate != null && compareEndDate!!.isBefore(compareStartDate)) {
            throw BusinessRuleException("Comparative end date must be on or after comparative start date")
        }

        val accounts =
            accountRepository
                .findByOrganizationIdAndIsActive(organizationId, true)
                .filter { it.type == AccountType.REVENUE || it.type == AccountType.EXPENSE }
        val currentTotals = computePeriodTotals(organizationId, startDate, endDate)
        val comparativeTotals =
            if (compareStartDate != null && compareEndDate != null) {
                computePeriodTotals(organizationId, compareStartDate, compareEndDate)
            } else {
                null
            }

        val revenueLines = buildLines(accounts, AccountType.REVENUE, currentTotals, comparativeTotals)
        val expenseLines = buildLines(accounts, AccountType.EXPENSE, currentTotals, comparativeTotals)

        val totalRevenue = revenueLines.fold(BigDecimal.ZERO) { acc, l -> acc.add(l.amount) }
        val totalExpenses = expenseLines.fold(BigDecimal.ZERO) { acc, l -> acc.add(l.amount) }
        val comparativeTotalRevenue =
            comparativeTotals?.let {
                revenueLines.fold(BigDecimal.ZERO) { acc, l -> acc.add(l.comparativeAmount ?: BigDecimal.ZERO) }
            }
        val comparativeTotalExpenses =
            comparativeTotals?.let {
                expenseLines.fold(BigDecimal.ZERO) { acc, l -> acc.add(l.comparativeAmount ?: BigDecimal.ZERO) }
            }

        return IncomeStatementResponse(
            startDate = startDate.toString(),
            endDate = endDate.toString(),
            comparativePeriod =
                if (compareStartDate != null && compareEndDate != null) {
                    ComparativePeriodMeta(compareStartDate.toString(), compareEndDate.toString())
                } else {
                    null
                },
            revenue = revenueLines,
            totalRevenue = totalRevenue,
            comparativeTotalRevenue = comparativeTotalRevenue,
            expenses = expenseLines,
            totalExpenses = totalExpenses,
            comparativeTotalExpenses = comparativeTotalExpenses,
            netIncome = totalRevenue.subtract(totalExpenses),
            comparativeNetIncome =
                if (comparativeTotalRevenue != null && comparativeTotalExpenses != null) {
                    comparativeTotalRevenue.subtract(comparativeTotalExpenses)
                } else {
                    null
                },
        )
    }

    fun getBalanceSheet(
        organizationId: java.util.UUID,
        asOfDate: LocalDate,
        compareAsOfDate: LocalDate? = null,
    ): BalanceSheetResponse {
        val accounts = accountRepository.findByOrganizationIdAndIsActive(organizationId, true)
        val current = computePeriodTotals(organizationId, null, asOfDate)
        val comparative = compareAsOfDate?.let { computePeriodTotals(organizationId, null, it) }

        val assets = buildLines(accounts, AccountType.ASSET, current, comparative)
        val liabilities = buildLines(accounts, AccountType.LIABILITY, current, comparative)
        val equityAccounts = buildLines(accounts, AccountType.EQUITY, current, comparative)

        val currentEarnings = computeCurrentEarnings(accounts, current)
        val comparativeCurrentEarnings = comparative?.let { computeCurrentEarnings(accounts, it) }

        val earningsLine =
            ReportAccountLine(
                accountId = SyntheticAccountIds.CURRENT_PERIOD_EARNINGS_ID,
                accountCode = SyntheticAccountIds.CURRENT_PERIOD_EARNINGS,
                accountName = "Current Period Earnings",
                amount = currentEarnings,
                comparativeAmount = comparativeCurrentEarnings,
                isSynthetic = true,
            )
        val equity = equityAccounts + earningsLine

        val totalAssets = assets.fold(BigDecimal.ZERO) { acc, l -> acc.add(l.amount) }
        val totalLiabilities = liabilities.fold(BigDecimal.ZERO) { acc, l -> acc.add(l.amount) }
        val totalEquity = equity.fold(BigDecimal.ZERO) { acc, l -> acc.add(l.amount) }
        val totalLiabilitiesAndEquity = totalLiabilities.add(totalEquity)
        val outOfBalance = totalAssets.subtract(totalLiabilitiesAndEquity)

        val comparativeTotalAssets =
            comparative?.let { assets.fold(BigDecimal.ZERO) { a, l -> a.add(l.comparativeAmount ?: BigDecimal.ZERO) } }
        val comparativeTotalLiabilities =
            comparative?.let {
                liabilities.fold(BigDecimal.ZERO) { a, l -> a.add(l.comparativeAmount ?: BigDecimal.ZERO) }
            }
        val comparativeTotalEquity =
            comparative?.let { equity.fold(BigDecimal.ZERO) { a, l -> a.add(l.comparativeAmount ?: BigDecimal.ZERO) } }
        val comparativeTotalLiabAndEq =
            if (comparativeTotalLiabilities != null && comparativeTotalEquity != null) {
                comparativeTotalLiabilities.add(comparativeTotalEquity)
            } else {
                null
            }

        return BalanceSheetResponse(
            asOfDate = asOfDate.toString(),
            comparativeAsOfDate = compareAsOfDate?.toString(),
            assets = assets,
            totalAssets = totalAssets,
            comparativeTotalAssets = comparativeTotalAssets,
            liabilities = liabilities,
            totalLiabilities = totalLiabilities,
            comparativeTotalLiabilities = comparativeTotalLiabilities,
            equity = equity,
            totalEquity = totalEquity,
            comparativeTotalEquity = comparativeTotalEquity,
            currentEarnings = currentEarnings,
            comparativeCurrentEarnings = comparativeCurrentEarnings,
            totalLiabilitiesAndEquity = totalLiabilitiesAndEquity,
            comparativeTotalLiabilitiesAndEquity = comparativeTotalLiabAndEq,
            isBalanced = outOfBalance.abs() < BigDecimal("0.01"),
            outOfBalanceAmount = outOfBalance,
        )
    }

    private fun computeCurrentEarnings(
        accounts: List<Account>,
        totals: Map<java.util.UUID, Pair<BigDecimal, BigDecimal>>,
    ): BigDecimal {
        var earnings = BigDecimal.ZERO
        accounts
            .filter { it.type == AccountType.REVENUE || it.type == AccountType.EXPENSE }
            .forEach { account ->
                val (debits, credits) = totals.getOrDefault(account.id, BigDecimal.ZERO to BigDecimal.ZERO)
                val signed = account.type.signedBalance(debits, credits)
                earnings =
                    if (account.type == AccountType.REVENUE) {
                        earnings.add(signed)
                    } else {
                        earnings.subtract(signed)
                    }
            }
        return earnings
    }

    private fun buildLines(
        accounts: List<Account>,
        type: AccountType,
        current: Map<java.util.UUID, Pair<BigDecimal, BigDecimal>>,
        comparative: Map<java.util.UUID, Pair<BigDecimal, BigDecimal>>?,
    ): List<ReportAccountLine> =
        accounts
            .filter { it.type == type }
            .map { account ->
                val (debits, credits) = current.getOrDefault(account.id, BigDecimal.ZERO to BigDecimal.ZERO)
                val amount = account.type.signedBalance(debits, credits)
                val compAmount =
                    comparative?.let {
                        val (cd, cc) = it.getOrDefault(account.id, BigDecimal.ZERO to BigDecimal.ZERO)
                        account.type.signedBalance(cd, cc)
                    }
                ReportAccountLine(
                    accountId = account.id,
                    accountCode = account.code,
                    accountName = account.name,
                    amount = amount,
                    comparativeAmount = compAmount,
                )
            }.sortedBy { it.accountCode }

    private fun computePeriodTotals(
        organizationId: java.util.UUID,
        startDate: LocalDate?,
        endDate: LocalDate,
    ): Map<java.util.UUID, Pair<BigDecimal, BigDecimal>> =
        journalEntryRepository
            .aggregateAccountTotals(organizationId, null, startDate, endDate)
            .mapValues { (_, totals) -> totals.totalDebits to totals.totalCredits }
}
