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
@Table(name = "bill_payments")
@EntityListeners(AuditingEntityListener::class)
data class BillPayment(
    @Id
    @Column(columnDefinition = "uuid")
    val id: String = UUID.randomUUID().toString(),
    @Column(name = "bill_id", columnDefinition = "uuid")
    val billId: String,
    @Column(name = "payment_date")
    val paymentDate: LocalDate,
    val amount: BigDecimal,
    @Column(name = "base_currency_amount")
    val baseCurrencyAmount: BigDecimal = amount,
    @Column(name = "exchange_rate")
    val exchangeRate: BigDecimal = BigDecimal.ONE,
    @Column(name = "payment_method")
    @Enumerated(EnumType.STRING)
    val paymentMethod: PaymentMethod,
    @Column(name = "reference_number")
    val referenceNumber: String? = null,
    @Column(name = "journal_entry_id", columnDefinition = "uuid")
    val journalEntryId: String? = null,
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: String,
    @Column(name = "created_by", columnDefinition = "uuid")
    val createdBy: String,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
)
