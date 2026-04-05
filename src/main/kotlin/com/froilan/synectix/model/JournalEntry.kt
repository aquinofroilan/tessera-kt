package com.froilan.synectix.model

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

enum class JournalEntryStatus {
    DRAFT,
    POSTED,
    VOIDED,
}

enum class JournalEntrySource {
    MANUAL,
    SYSTEM,
}

data class JournalEntryLine(
    val accountId: String,
    val accountCode: String,
    val accountName: String,
    val debit: BigDecimal = BigDecimal.ZERO,
    val credit: BigDecimal = BigDecimal.ZERO,
    val description: String? = null,
)

@Document(collection = "journal_entries")
@CompoundIndex(
    name = "unique_entry_number_per_org",
    def = "{'organizationId': 1, 'entryNumber': 1}",
    unique = true,
)
data class JournalEntry(
    @Id
    val id: String = UUID.randomUUID().toString(),
    val entryNumber: String,
    val date: LocalDate,
    val description: String,
    @Indexed
    val organizationId: String,
    val status: JournalEntryStatus = JournalEntryStatus.DRAFT,
    val source: JournalEntrySource = JournalEntrySource.MANUAL,
    val sourceReference: String? = null,
    val lines: List<JournalEntryLine>,
    val createdBy: String,
    val postedAt: LocalDateTime? = null,
    val voidedAt: LocalDateTime? = null,
    val voidReason: String? = null,
    @CreatedDate
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    var updatedAt: LocalDateTime? = null,
)
