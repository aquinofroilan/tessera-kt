package com.aquinofroilan.tessera.domain.finance.model

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
class JournalEntryLine(
    @Id
    @Column(columnDefinition = "uuid")
    var id: java.util.UUID = java.util.UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "line_number")
    var lineNumber: Int = 0,
    @Column(name = "account_id", columnDefinition = "uuid")
    var accountId: java.util.UUID,
    @Column(name = "account_code")
    var accountCode: String,
    @Column(name = "account_name")
    var accountName: String,
    var debit: BigDecimal = BigDecimal.ZERO,
    var credit: BigDecimal = BigDecimal.ZERO,
    var description: String? = null,
)

@Entity
@Table(name = "journal_entries")
@EntityListeners(AuditingEntityListener::class)
class JournalEntry(
    @Id
    @Column(columnDefinition = "uuid")
    var id: java.util.UUID = java.util.UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "entry_number")
    var entryNumber: String,
    var date: LocalDate,
    var description: String,
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: java.util.UUID,
    @Enumerated(EnumType.STRING)
    var status: JournalEntryStatus = JournalEntryStatus.DRAFT,
    @Enumerated(EnumType.STRING)
    var source: JournalEntrySource = JournalEntrySource.MANUAL,
    @Column(name = "source_reference")
    var sourceReference: String? = null,
    @OneToMany(
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.EAGER,
    )
    @JoinColumn(name = "journal_entry_id")
    @OrderBy("lineNumber ASC")
    var lines: List<JournalEntryLine>,
    @Column(name = "created_by", columnDefinition = "uuid")
    var createdBy: java.util.UUID,
    @Column(name = "posted_at")
    var postedAt: LocalDateTime? = null,
    @Column(name = "voided_at")
    var voidedAt: LocalDateTime? = null,
    @Column(name = "void_reason")
    var voidReason: String? = null,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)
