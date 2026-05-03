package com.froilan.synectix.repository

import com.froilan.synectix.model.JournalEntry
import com.froilan.synectix.model.JournalEntryStatus
import org.bson.Document
import org.bson.types.Decimal128
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.query.Criteria
import java.math.BigDecimal
import java.time.LocalDate

data class AccountTotals(
    val totalDebits: BigDecimal,
    val totalCredits: BigDecimal,
)

interface JournalEntryAggregations {
    fun aggregateAccountTotals(
        organizationId: String,
        accountIds: Collection<String>?,
        startDate: LocalDate?,
        endDate: LocalDate?,
    ): Map<String, AccountTotals>
}

open class JournalEntryAggregationsImpl(
    private val mongoTemplate: MongoTemplate,
) : JournalEntryAggregations {
    override fun aggregateAccountTotals(
        organizationId: String,
        accountIds: Collection<String>?,
        startDate: LocalDate?,
        endDate: LocalDate?,
    ): Map<String, AccountTotals> {
        if (accountIds != null && accountIds.isEmpty()) {
            return emptyMap()
        }
        val entryCriteria =
            Criteria()
                .and("organizationId")
                .`is`(organizationId)
                .and("status")
                .`in`(JournalEntryStatus.POSTED.name, JournalEntryStatus.VOIDED.name)
        if (startDate != null && endDate != null) {
            entryCriteria.and("date").gte(startDate).lte(endDate)
        } else if (startDate != null) {
            entryCriteria.and("date").gte(startDate)
        } else if (endDate != null) {
            entryCriteria.and("date").lte(endDate)
        }

        val stages = mutableListOf<org.springframework.data.mongodb.core.aggregation.AggregationOperation>()
        stages.add(Aggregation.match(entryCriteria))
        stages.add(Aggregation.unwind("lines"))
        if (accountIds != null) {
            stages.add(Aggregation.match(Criteria.where("lines.accountId").`in`(accountIds)))
        }
        stages.add(
            Aggregation
                .group("lines.accountId")
                .sum("lines.debit")
                .`as`("totalDebits")
                .sum("lines.credit")
                .`as`("totalCredits"),
        )

        val results =
            mongoTemplate.aggregate(
                Aggregation.newAggregation(stages),
                JournalEntry::class.java,
                Document::class.java,
            )

        return results.mappedResults.associate { doc ->
            val accountId = doc.getString("_id")
            val debits = toBigDecimal(doc.get("totalDebits"))
            val credits = toBigDecimal(doc.get("totalCredits"))
            accountId to AccountTotals(debits, credits)
        }
    }

    private fun toBigDecimal(value: Any?): BigDecimal =
        when (value) {
            null -> BigDecimal.ZERO
            is Decimal128 -> value.bigDecimalValue()
            is BigDecimal -> value
            is Number -> BigDecimal(value.toString())
            is String -> value.toBigDecimalOrNull() ?: BigDecimal.ZERO
            else -> BigDecimal.ZERO
        }
}
