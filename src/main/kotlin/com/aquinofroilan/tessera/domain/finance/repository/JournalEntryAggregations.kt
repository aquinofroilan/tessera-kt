package com.aquinofroilan.tessera.domain.finance.repository

import org.springframework.jdbc.core.JdbcTemplate
import java.math.BigDecimal
import java.time.LocalDate

data class AccountTotals(
    val totalDebits: BigDecimal,
    val totalCredits: BigDecimal,
)

interface JournalEntryAggregations {
    fun aggregateAccountTotals(
        organizationId: java.util.UUID,
        accountIds: Collection<java.util.UUID>?,
        startDate: LocalDate?,
        endDate: LocalDate?,
    ): Map<java.util.UUID, AccountTotals>
}

open class JournalEntryAggregationsImpl(
    private val jdbc: JdbcTemplate,
) : JournalEntryAggregations {
    override fun aggregateAccountTotals(
        organizationId: java.util.UUID,
        accountIds: Collection<java.util.UUID>?,
        startDate: LocalDate?,
        endDate: LocalDate?,
    ): Map<java.util.UUID, AccountTotals> {
        if (accountIds != null && accountIds.isEmpty()) {
            return emptyMap()
        }

        val params = mutableListOf<Any>()
        params.add(organizationId)

        val sql =
            buildString {
                append(
                    """
                    SELECT l.account_id,
                           COALESCE(SUM(l.debit), 0)  AS total_debits,
                           COALESCE(SUM(l.credit), 0) AS total_credits
                      FROM journal_entry_lines l
                      JOIN journal_entries e ON e.id = l.journal_entry_id
                     WHERE e.organization_id = ?::uuid
                       AND e.status IN ('POSTED', 'VOIDED')
                    """.trimIndent(),
                )
                if (startDate != null) {
                    append(" AND e.date >= ?")
                    params.add(startDate)
                }
                if (endDate != null) {
                    append(" AND e.date <= ?")
                    params.add(endDate)
                }
                if (accountIds != null) {
                    val placeholders = accountIds.joinToString(",") { "?::uuid" }
                    append(" AND l.account_id IN ($placeholders)")
                    params.addAll(accountIds)
                }
                append(" GROUP BY l.account_id")
            }

        val totals = mutableMapOf<java.util.UUID, AccountTotals>()
        jdbc.query(sql, { rs ->
            val accountId = java.util.UUID.fromString(rs.getString("account_id"))
            val debits = rs.getBigDecimal("total_debits") ?: BigDecimal.ZERO
            val credits = rs.getBigDecimal("total_credits") ?: BigDecimal.ZERO
            totals[accountId] = AccountTotals(debits, credits)
        }, *params.toTypedArray())

        return totals
    }
}
