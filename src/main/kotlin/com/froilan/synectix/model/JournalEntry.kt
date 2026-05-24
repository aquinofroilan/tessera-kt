package com.froilan.synectix.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
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

@Entity
@Table(name = "journal_entry_lines")
data class JournalEntryLine(
    @Id
    @Column(columnDefinition = "uuid")
    val id: String = UUID.randomUUID().toString(),
    @Column(name = "line_number")
    val lineNumber: Int = 0,
    @Column(name = "account_id", columnDefinition = "uuid")
    val accountId: String,
    @Column(name = "account_code")
    val accountCode: String,
    @Column(name = "account_name")
    val accountName: String,
    val debit: BigDecimal = BigDecimal.ZERO,
    val credit: BigDecimal = BigDecimal.ZERO,
    val description: String? = null,
)

@Entity
@Table(name = "journal_entries")
@EntityListeners(AuditingEntityListener::class)
data class JournalEntry(
    @Id
    @Column(columnDefinition = "uuid")
    val id: String = UUID.randomUUID().toString(),
    @Column(name = "entry_number")
    val entryNumber: String,
    val date: LocalDate,
    val description: String,
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: String,
    @Enumerated(EnumType.STRING)
    val status: JournalEntryStatus = JournalEntryStatus.DRAFT,
    @Enumerated(EnumType.STRING)
    val source: JournalEntrySource = JournalEntrySource.MANUAL,
    @Column(name = "source_reference")
    val sourceReference: String? = null,
    @OneToMany(
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.EAGER,
    )
    @JoinColumn(name = "journal_entry_id")
    @OrderBy("lineNumber ASC")
    val lines: List<JournalEntryLine>,
    @Column(name = "created_by", columnDefinition = "uuid")
    val createdBy: String,
    @Column(name = "posted_at")
    val postedAt: LocalDateTime? = null,
    @Column(name = "voided_at")
    val voidedAt: LocalDateTime? = null,
    @Column(name = "void_reason")
    val voidReason: String? = null,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)
