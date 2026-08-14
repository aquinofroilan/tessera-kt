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

enum class InvoiceStatus {
    DRAFT,
    APPROVED,
    PARTIALLY_PAID,
    PAID,
    VOID,
}

@Entity
@Table(name = "invoice_lines")
class InvoiceLine(
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
    var amount: BigDecimal,
    var description: String? = null,
)

@Entity
@Table(name = "invoices")
@EntityListeners(AuditingEntityListener::class)
class Invoice(
    @Id
    @Column(columnDefinition = "uuid")
    var id: java.util.UUID = java.util.UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "invoice_number")
    var invoiceNumber: String,
    @Column(name = "customer_id", columnDefinition = "uuid")
    var customerId: java.util.UUID,
    @Column(name = "customer_name")
    var customerName: String,
    var date: LocalDate,
    @Column(name = "due_date")
    var dueDate: LocalDate,
    @Column(name = "reference_number")
    var referenceNumber: String? = null,
    @Column(name = "tax_group_id", columnDefinition = "uuid")
    var taxGroupId: java.util.UUID? = null,
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: java.util.UUID,
    @Enumerated(EnumType.STRING)
    var status: InvoiceStatus = InvoiceStatus.DRAFT,
    @OneToMany(
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.EAGER,
    )
    @JoinColumn(name = "invoice_id")
    @OrderBy("lineNumber ASC")
    var lines: List<InvoiceLine>,
    @Column(name = "total_amount")
    var totalAmount: BigDecimal,
    @Column(name = "tax_amount")
    var taxAmount: BigDecimal = BigDecimal.ZERO,
    @Column(name = "amount_received")
    var amountReceived: BigDecimal = BigDecimal.ZERO,
    @Column(name = "currency_code", columnDefinition = "char(3)")
    var currencyCode: String = "USD",
    @Column(name = "exchange_rate")
    var exchangeRate: BigDecimal = BigDecimal.ONE,
    @Column(name = "base_currency_amount")
    var baseCurrencyAmount: BigDecimal = totalAmount,
    @Column(name = "base_currency_tax_amount")
    var baseCurrencyTaxAmount: BigDecimal = taxAmount,
    @Column(name = "base_currency_amount_received")
    var baseCurrencyAmountReceived: BigDecimal = amountReceived,
    @Column(name = "journal_entry_id", columnDefinition = "uuid")
    var journalEntryId: java.util.UUID? = null,
    @Column(name = "created_by", columnDefinition = "uuid")
    var createdBy: java.util.UUID,
    @Column(name = "approved_at")
    var approvedAt: LocalDateTime? = null,
    @Column(name = "approved_by", columnDefinition = "uuid")
    var approvedBy: java.util.UUID? = null,
    @Column(name = "paid_at")
    var paidAt: LocalDateTime? = null,
    @Column(name = "voided_at")
    var voidedAt: LocalDateTime? = null,
    @Column(name = "voided_by", columnDefinition = "uuid")
    var voidedBy: java.util.UUID? = null,
    @Column(name = "void_reason")
    var voidReason: String? = null,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)
