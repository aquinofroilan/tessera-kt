package com.aquinofroilan.tessera.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "invoice_receipts")
@EntityListeners(AuditingEntityListener::class)
class InvoiceReceipt(
    @Id
    @Column(columnDefinition = "uuid")
    var id: java.util.UUID = UUID.randomUUID(),
    @Column(name = "invoice_id", columnDefinition = "uuid")
    var invoiceId: java.util.UUID,
    @Column(name = "receipt_date")
    var receiptDate: LocalDate,
    var amount: BigDecimal,
    @Column(name = "base_currency_amount")
    var baseCurrencyAmount: BigDecimal = amount,
    @Column(name = "exchange_rate")
    var exchangeRate: BigDecimal = BigDecimal.ONE,
    @Column(name = "payment_method")
    @Enumerated(EnumType.STRING)
    var paymentMethod: PaymentMethod,
    @Column(name = "reference_number")
    var referenceNumber: String? = null,
    @Column(name = "journal_entry_id", columnDefinition = "uuid")
    var journalEntryId: java.util.UUID? = null,
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: java.util.UUID,
    @Column(name = "created_by", columnDefinition = "uuid")
    var createdBy: java.util.UUID,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
)
