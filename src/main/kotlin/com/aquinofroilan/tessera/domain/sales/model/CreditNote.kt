package com.aquinofroilan.tessera.domain.sales.model

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
import java.time.ZoneOffset
import java.util.UUID

enum class CreditNoteStatus {
    DRAFT,
    APPROVED,
    PARTIALLY_APPLIED,
    APPLIED,
    VOID,
}

@Entity
@Table(name = "credit_note_lines")
class CreditNoteLine(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "credit_note_id", nullable = false, columnDefinition = "uuid")
    var creditNoteId: UUID,
    @Column(name = "line_number", nullable = false)
    var lineNumber: Int = 0,
    @Column(name = "product_id", columnDefinition = "uuid")
    var productId: UUID? = null,
    @Column(nullable = false)
    var description: String,
    @Column(nullable = false, precision = 19, scale = 4)
    var quantity: BigDecimal = BigDecimal.ONE,
    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    var unitPrice: BigDecimal,
    @Column(name = "line_total", nullable = false, precision = 19, scale = 4)
    var lineTotal: BigDecimal,
    @Column(name = "account_id", columnDefinition = "uuid")
    var accountId: UUID? = null,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
)

@Entity
@Table(name = "credit_note_allocations")
class CreditNoteAllocation(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "organization_id", nullable = false, columnDefinition = "uuid")
    var organizationId: UUID,
    @Column(name = "credit_note_id", nullable = false, columnDefinition = "uuid")
    var creditNoteId: UUID,
    @Column(name = "invoice_id", nullable = false, columnDefinition = "uuid")
    var invoiceId: UUID,
    @Column(name = "amount_applied", nullable = false, precision = 19, scale = 4)
    var amountApplied: BigDecimal,
    @Column(name = "applied_date", nullable = false)
    var appliedDate: LocalDate = LocalDate.now(),
    @Column(name = "applied_by", nullable = false, columnDefinition = "uuid")
    var appliedBy: UUID,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
)

@Entity
@Table(name = "credit_notes")
@EntityListeners(AuditingEntityListener::class)
class CreditNote(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "organization_id", nullable = false, columnDefinition = "uuid")
    var organizationId: UUID,
    @Column(name = "credit_note_number", nullable = false)
    var creditNoteNumber: String,
    @Column(name = "customer_id", nullable = false, columnDefinition = "uuid")
    var customerId: UUID,
    @Column(name = "customer_name", nullable = false)
    var customerName: String,
    @Column(name = "sales_return_id", columnDefinition = "uuid")
    var salesReturnId: UUID? = null,
    @Column(name = "invoice_id", columnDefinition = "uuid")
    var invoiceId: UUID? = null,
    @Column(nullable = false)
    var date: LocalDate = LocalDate.now(),
    @Column(nullable = false)
    var currency: String = "USD",
    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    var totalAmount: BigDecimal,
    @Column(name = "allocated_amount", nullable = false, precision = 19, scale = 4)
    var allocatedAmount: BigDecimal = BigDecimal.ZERO,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: CreditNoteStatus = CreditNoteStatus.DRAFT,
    var reason: String? = null,
    @Column(name = "created_by", nullable = false, columnDefinition = "uuid")
    var createdBy: UUID,
    @Column(name = "approved_by", columnDefinition = "uuid")
    var approvedBy: UUID? = null,
    @Column(name = "approved_at")
    var approvedAt: LocalDateTime? = null,
    @Column(name = "journal_entry_id", columnDefinition = "uuid")
    var journalEntryId: UUID? = null,
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "credit_note_id")
    @OrderBy("lineNumber ASC")
    var lines: MutableList<CreditNoteLine> = mutableListOf(),
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "credit_note_id")
    var allocations: MutableList<CreditNoteAllocation> = mutableListOf(),
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
)
