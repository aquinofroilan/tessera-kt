package com.aquinofroilan.tessera.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
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

@Entity
@Table(name = "finance_bank_statement_lines")
data class BankStatementLine(
    @Id
    @Column(columnDefinition = "uuid")
    val id: String = UUID.randomUUID().toString(),
    @Column(name = "line_number")
    val lineNumber: Int,
    @Column(name = "posted_date")
    val postedDate: LocalDate,
    val description: String,
    val reference: String? = null,
    val amount: BigDecimal,
    val reconciled: Boolean = false,
    @Column(name = "reconciled_journal_entry_id", columnDefinition = "uuid")
    val reconciledJournalEntryId: String? = null,
    @Column(name = "reconciled_at")
    val reconciledAt: LocalDateTime? = null,
    @Column(name = "reconciled_by", columnDefinition = "uuid")
    val reconciledBy: String? = null,
)

@Entity
@Table(name = "finance_bank_statements")
@EntityListeners(AuditingEntityListener::class)
data class BankStatement(
    @Id
    @Column(columnDefinition = "uuid")
    val id: String = UUID.randomUUID().toString(),
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: String,
    @Column(name = "bank_account_id", columnDefinition = "uuid")
    val bankAccountId: String,
    @Column(name = "statement_date")
    val statementDate: LocalDate,
    @Column(name = "opening_balance")
    val openingBalance: BigDecimal,
    @Column(name = "closing_balance")
    val closingBalance: BigDecimal,
    @Column(columnDefinition = "char(3)")
    val currency: String,
    val source: String = "CSV",
    @Column(name = "uploaded_by", columnDefinition = "uuid")
    val uploadedBy: String,
    val notes: String? = null,
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "statement_id")
    @OrderBy("lineNumber ASC")
    val lines: List<BankStatementLine>,
    @CreatedDate
    @Column(name = "created_at")
    val createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,
)
