package com.aquinofroilan.tessera.model

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

enum class InvoiceStatus {
    DRAFT,
    APPROVED,
    PARTIALLY_PAID,
    PAID,
    VOID,
}

@Entity
@Table(name = "invoice_lines")
data class InvoiceLine(
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
    val amount: BigDecimal,
    val description: String? = null,
)

@Entity
@Table(name = "invoices")
@EntityListeners(AuditingEntityListener::class)
data class Invoice(
    @Id
    @Column(columnDefinition = "uuid")
    val id: String = UUID.randomUUID().toString(),
    @Column(name = "invoice_number")
    val invoiceNumber: String,
    @Column(name = "customer_id", columnDefinition = "uuid")
    val customerId: String,
    @Column(name = "customer_name")
    val customerName: String,
    val date: LocalDate,
    @Column(name = "due_date")
    val dueDate: LocalDate,
    @Column(name = "reference_number")
    val referenceNumber: String? = null,
    @Column(name = "tax_group_id", columnDefinition = "uuid")
    val taxGroupId: String? = null,
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: String,
    @Enumerated(EnumType.STRING)
    val status: InvoiceStatus = InvoiceStatus.DRAFT,
    @OneToMany(
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.EAGER,
    )
    @JoinColumn(name = "invoice_id")
    @OrderBy("lineNumber ASC")
    val lines: List<InvoiceLine>,
    @Column(name = "total_amount")
    val totalAmount: BigDecimal,
    @Column(name = "tax_amount")
    val taxAmount: BigDecimal = BigDecimal.ZERO,
    @Column(name = "amount_received")
    val amountReceived: BigDecimal = BigDecimal.ZERO,
    @Column(name = "currency_code", columnDefinition = "char(3)")
    val currencyCode: String = "USD",
    @Column(name = "exchange_rate")
    val exchangeRate: BigDecimal = BigDecimal.ONE,
    @Column(name = "base_currency_amount")
    val baseCurrencyAmount: BigDecimal = totalAmount,
    @Column(name = "base_currency_tax_amount")
    val baseCurrencyTaxAmount: BigDecimal = taxAmount,
    @Column(name = "base_currency_amount_received")
    val baseCurrencyAmountReceived: BigDecimal = amountReceived,
    @Column(name = "journal_entry_id", columnDefinition = "uuid")
    val journalEntryId: String? = null,
    @Column(name = "created_by", columnDefinition = "uuid")
    val createdBy: String,
    @Column(name = "approved_at")
    val approvedAt: LocalDateTime? = null,
    @Column(name = "approved_by", columnDefinition = "uuid")
    val approvedBy: String? = null,
    @Column(name = "paid_at")
    val paidAt: LocalDateTime? = null,
    @Column(name = "voided_at")
    val voidedAt: LocalDateTime? = null,
    @Column(name = "voided_by", columnDefinition = "uuid")
    val voidedBy: String? = null,
    @Column(name = "void_reason")
    val voidReason: String? = null,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)
