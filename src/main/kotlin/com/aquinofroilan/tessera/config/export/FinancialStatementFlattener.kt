package com.aquinofroilan.tessera.config.export

import com.aquinofroilan.tessera.domain.finance.dto.ApAgingReportResponse
import com.aquinofroilan.tessera.domain.finance.dto.ArAgingReportResponse
import com.aquinofroilan.tessera.domain.finance.dto.BalanceSheetResponse
import com.aquinofroilan.tessera.domain.finance.dto.ComparativeTrialBalanceResponse
import com.aquinofroilan.tessera.domain.finance.dto.IncomeStatementResponse
import com.aquinofroilan.tessera.domain.finance.dto.TrialBalanceResponse
import java.math.BigDecimal

object FinancialStatementFlattener {
    fun flattenTrialBalance(report: TrialBalanceResponse): List<Map<String, Any?>> {
        val rows = mutableListOf<Map<String, Any?>>()
        report.accounts.forEach { acc ->
            rows.add(
                mapOf(
                    "Code" to acc.accountCode,
                    "Name" to acc.accountName,
                    "Type" to acc.accountType,
                    "Debits" to acc.totalDebits,
                    "Credits" to acc.totalCredits,
                    "Balance" to acc.balance,
                ),
            )
        }
        rows.add(
            mapOf(
                "Code" to "TOTAL",
                "Name" to "",
                "Type" to "",
                "Debits" to report.totalDebits,
                "Credits" to report.totalCredits,
                "Balance" to "",
            ),
        )
        return rows
    }

    fun flattenComparativeTrialBalance(report: ComparativeTrialBalanceResponse): List<Map<String, Any?>> {
        val rows = mutableListOf<Map<String, Any?>>()

        // Build map of comparative accounts
        val compAccounts = report.comparative?.accounts?.associateBy { it.accountId } ?: emptyMap()

        report.current.accounts.forEach { acc ->
            val comp = compAccounts[acc.accountId]
            rows.add(
                mapOf(
                    "Code" to acc.accountCode,
                    "Name" to acc.accountName,
                    "Type" to acc.accountType,
                    "Current Debits" to acc.totalDebits,
                    "Current Credits" to acc.totalCredits,
                    "Current Balance" to acc.balance,
                    "Comparative Debits" to (comp?.totalDebits ?: BigDecimal.ZERO),
                    "Comparative Credits" to (comp?.totalCredits ?: BigDecimal.ZERO),
                    "Comparative Balance" to (comp?.balance ?: BigDecimal.ZERO),
                ),
            )
        }

        rows.add(
            mapOf(
                "Code" to "TOTAL",
                "Name" to "",
                "Type" to "",
                "Current Debits" to report.current.totalDebits,
                "Current Credits" to report.current.totalCredits,
                "Current Balance" to "",
                "Comparative Debits" to (report.comparative?.totalDebits ?: BigDecimal.ZERO),
                "Comparative Credits" to (report.comparative?.totalCredits ?: BigDecimal.ZERO),
                "Comparative Balance" to "",
            ),
        )
        return rows
    }

    fun flattenIncomeStatement(report: IncomeStatementResponse): List<Map<String, Any?>> {
        val rows = mutableListOf<Map<String, Any?>>()

        val hasComp = report.comparativePeriod != null

        fun addRow(
            name: String,
            amt: BigDecimal?,
            compAmt: BigDecimal?,
            isHeader: Boolean = false,
        ) {
            val map = mutableMapOf<String, Any?>("Account" to name)
            if (isHeader) {
                map["Amount"] = ""
                if (hasComp) map["Comparative"] = ""
            } else {
                map["Amount"] = amt ?: BigDecimal.ZERO
                if (hasComp) map["Comparative"] = compAmt ?: BigDecimal.ZERO
            }
            rows.add(map)
        }

        addRow("REVENUE", null, null, true)
        report.revenue.forEach { addRow("  ${it.accountName}", it.amount, it.comparativeAmount) }
        addRow("TOTAL REVENUE", report.totalRevenue, report.comparativeTotalRevenue)

        addRow("", null, null, true)

        addRow("EXPENSES", null, null, true)
        report.expenses.forEach { addRow("  ${it.accountName}", it.amount, it.comparativeAmount) }
        addRow("TOTAL EXPENSES", report.totalExpenses, report.comparativeTotalExpenses)

        addRow("", null, null, true)
        addRow("NET INCOME", report.netIncome, report.comparativeNetIncome)

        return rows
    }

    fun flattenBalanceSheet(report: BalanceSheetResponse): List<Map<String, Any?>> {
        val rows = mutableListOf<Map<String, Any?>>()
        val hasComp = report.comparativeAsOfDate != null

        fun addRow(
            name: String,
            amt: BigDecimal?,
            compAmt: BigDecimal?,
            isHeader: Boolean = false,
        ) {
            val map = mutableMapOf<String, Any?>("Account" to name)
            if (isHeader) {
                map["Amount"] = ""
                if (hasComp) map["Comparative"] = ""
            } else {
                map["Amount"] = amt ?: BigDecimal.ZERO
                if (hasComp) map["Comparative"] = compAmt ?: BigDecimal.ZERO
            }
            rows.add(map)
        }

        addRow("ASSETS", null, null, true)
        report.assets.forEach { addRow("  ${it.accountName}", it.amount, it.comparativeAmount) }
        addRow("TOTAL ASSETS", report.totalAssets, report.comparativeTotalAssets)

        addRow("", null, null, true)

        addRow("LIABILITIES", null, null, true)
        report.liabilities.forEach { addRow("  ${it.accountName}", it.amount, it.comparativeAmount) }
        addRow("TOTAL LIABILITIES", report.totalLiabilities, report.comparativeTotalLiabilities)

        addRow("", null, null, true)

        addRow("EQUITY", null, null, true)
        report.equity.forEach { addRow("  ${it.accountName}", it.amount, it.comparativeAmount) }
        addRow("TOTAL EQUITY", report.totalEquity, report.comparativeTotalEquity)

        addRow("", null, null, true)
        addRow("TOTAL LIABILITIES & EQUITY", report.totalLiabilitiesAndEquity, report.comparativeTotalLiabilitiesAndEquity)

        return rows
    }

    fun flattenArAging(report: ArAgingReportResponse): List<Map<String, Any?>> {
        val rows = mutableListOf<Map<String, Any?>>()

        report.customers.forEach { cust ->
            rows.add(
                mapOf(
                    "Customer Name" to cust.customerName,
                    "Current" to cust.aging.current,
                    "1-30 Days" to cust.aging.days1to30,
                    "31-60 Days" to cust.aging.days31to60,
                    "61-90 Days" to cust.aging.days61to90,
                    "> 90 Days" to cust.aging.days90plus,
                    "Total" to cust.aging.total,
                ),
            )
        }

        rows.add(
            mapOf(
                "Customer Name" to "TOTAL",
                "Current" to report.totals.current,
                "1-30 Days" to report.totals.days1to30,
                "31-60 Days" to report.totals.days31to60,
                "61-90 Days" to report.totals.days61to90,
                "> 90 Days" to report.totals.days90plus,
                "Total" to report.totals.total,
            ),
        )

        return rows
    }

    fun flattenApAging(report: ApAgingReportResponse): List<Map<String, Any?>> {
        val rows = mutableListOf<Map<String, Any?>>()

        report.vendors.forEach { vend ->
            rows.add(
                mapOf(
                    "Vendor Name" to vend.vendorName,
                    "Current" to vend.aging.current,
                    "1-30 Days" to vend.aging.days1to30,
                    "31-60 Days" to vend.aging.days31to60,
                    "61-90 Days" to vend.aging.days61to90,
                    "> 90 Days" to vend.aging.days90plus,
                    "Total" to vend.aging.total,
                ),
            )
        }

        rows.add(
            mapOf(
                "Vendor Name" to "TOTAL",
                "Current" to report.totals.current,
                "1-30 Days" to report.totals.days1to30,
                "31-60 Days" to report.totals.days31to60,
                "61-90 Days" to report.totals.days61to90,
                "> 90 Days" to report.totals.days90plus,
                "Total" to report.totals.total,
            ),
        )

        return rows
    }
}
